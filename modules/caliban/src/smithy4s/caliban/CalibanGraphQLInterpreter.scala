/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.caliban

import caliban.interop.cats.implicits._
import caliban.schema._
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import smithy4s.Service

object CalibanGraphQLInterpreter {
  def server[Alg[_[_, _, _, _, _]], F[_]: Async: Dispatcher](implicit
      service: Service[Alg]
  ): Schema[Any, service.Impl[F]] =
    // todo: renaming to account for graphql conventions (camelCase ops etc.)?
    Schema.obj(name = service.id.name, description = None)(implicit fa =>
      service.endpoints.map { endpointToSchema[F].apply(service)(_) }
    )

  private def fToSchema[F[_]: Async: Dispatcher, O](
      schema: smithy4s.Schema[O]
  ): Schema[Any, F[O]] = {
    implicit val underlying: Schema[Any, O] =
      schema.compile(CalibanSchemaVisitor)
    implicitly
  }

  private def endpointToSchema[
      F[_]: Async: Dispatcher
  ] = new EndpointToSchemaPartiallyApplied[F]

  // "partially-applied type" pattern used here to give the compiler a hint about what F is
  // but let it infer the remaining type parameters
  final class EndpointToSchemaPartiallyApplied[
      F[_]: Async: Dispatcher
  ] private[caliban] {
    def apply[
        Alg[_[_, _, _, _, _]],
        I,
        E,
        O,
        SI,
        SO
    ](service: Service[Alg])(
        e: service.Endpoint[I, E, O, SI, SO]
    )(implicit fa: FieldAttributes) = {

      Schema.fieldWithArgs[service.Impl[F], I](e.name) { alg =>
        val interp = service.toPolyFunction(alg)

        i => interp(e.wrap(i))
      }(
        Schema.functionSchema[Any, Any, I, F[O]](
          e.input.compile(ArgBuilderVisitor),
          e.input.compile(CalibanSchemaVisitor),
          fToSchema(e.output)
        ),
        fa
      )
    }
  }
}
