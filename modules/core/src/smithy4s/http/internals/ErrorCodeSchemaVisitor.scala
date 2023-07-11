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

package smithy4s
package http
package internals

import smithy4s.schema._

private[http] class ErrorCodeSchemaVisitor(
    val cache: CompilationCache[HttpCode]
) extends SchemaVisitor.Cached[HttpCode]
    with SchemaVisitor.Default[HttpCode] { compile =>
  def default[A]: A => Option[Int] = _ => None

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatcher: Alt.Dispatcher[U]
  ): HttpCode[U] = {
    dispatcher.compile(new Alt.Precompiler[HttpCode] {
      def apply[A](label: String, instance: Schema[A]): HttpCode[A] =
        compile(instance)
    })
  }

  override def lazily[A](suspend: Lazy[Schema[A]]): HttpCode[A] =
    apply(suspend.value)

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): HttpCode[B] = {
    val httpCode = apply(schema)
    b => httpCode(bijection.from(b))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): HttpCode[B] = {
    val httpCode = apply(schema)
    b => httpCode(refinement.from(b))
  }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): HttpCode[S] = { _ =>
    hints
      .get(smithy.api.HttpError)
      .map(_.value)
      .orElse(hints.get(smithy.api.Error).map {
        case smithy.api.Error.CLIENT => 400
        case smithy.api.Error.SERVER => 500
      })
  }

}
