/*
 *  Copyright 2021-2025 Disney Streaming
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

import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.Kind5
import smithy4s.EndpointHandler.AsService

/**
  * Composable handler that allows to implement a specific endpoint in isolation.
  *
  * Handlers are composable and can be reconciled into the service the operations belong to.
  */
trait EndpointHandler[Op[_, _, _, _, _], F[_, _, _, _, _]] {
  import EndpointHandler.Combined

  protected[smithy4s] def lift[Alg[_[_, _, _, _, _]]](
      service: Service.Aux[Alg, Op]
  ): PolyFunction5[Op, Kind5[F]#optional]

  final def asService[Alg[_[_, _, _, _, _]]](
      service: Service.Aux[Alg, Op]
  ): AsService[Alg, F] =
    new EndpointHandler.AsServiceImpl[Alg, Op, F](this, service)

  final def or(other: EndpointHandler[Op, F]): EndpointHandler[Op, F] =
    (this, other) match {
      case (Combined(left), Combined(right)) => Combined(left ++ right)
      case (other, Combined(right))          => Combined(other +: right)
      case (Combined(left), other)           => Combined(left :+ other)
      case (left, right)                     => Combined(Vector(left, right))
    }
}

// scalafmt: { maxColumn = 120 }
object EndpointHandler {

  /**
    * Partial step when handlers are transformed into a service, allowing them to decide how to handle
    * un-implemented endpoints.
    */
  trait AsService[Alg[_[_, _, _, _, _]], F[_, _, _, _, _]] {

    /**
      * Returns an instance of the algebra that throws when one of the methods doesn't have a matching endpoint
      * handler
      */
    def throwing: Alg[F]

    /**
     * Returns an instance of the algebra that raises an error in an effect when one of the methods doesn't have a matching
     * endpoint handler
     */
    def failingWith(f: ShapeId => F[Any, Nothing, Nothing, Nothing, Nothing]): Alg[F]

    /**
      * Returns an instance of the algebra that wraps implemented methods in `Some` and return `None` on unimplemented methods
      */
    def partial: Alg[Kind5[F]#optional]
  }

  private class AsServiceImpl[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_, _, _, _, _]](
      handler: EndpointHandler[Op, F],
      service: Service.Aux[Alg, Op]
  ) extends AsService[Alg, F] {
    def throwing: Alg[F] = {
      val lifted = handler.lift(service)
      val interpreter = new PolyFunction5[Op, F] {
        def apply[I, E, O, SI, SO](op: Op[I, E, O, SI, SO]) =
          lifted(op).getOrElse {
            val endpointId = service.endpoint(op).id
            throw new NotImplementedError(endpointId.show)
          }
      }
      service.fromPolyFunction(interpreter)
    }

    def failingWith(f: ShapeId => F[Any, Nothing, Nothing, Nothing, Nothing]): Alg[F] = {
      val lifted = handler.lift(service)
      val interpreter = new PolyFunction5[Op, F] {
        def apply[I, E, O, SI, SO](op: Op[I, E, O, SI, SO]) =
          lifted(op).getOrElse {
            val endpointId = service.endpoint(op).id
            f(endpointId).asInstanceOf[F[I, E, O, SI, SO]]
          }
      }
      service.fromPolyFunction(interpreter)
    }

    def partial: Alg[Kind5[F]#optional] =
      service.fromPolyFunction[Kind5[F]#optional](handler.lift(service))
  }

  private[smithy4s] def combineAll[Op[_, _, _, _, _], F[_, _, _, _, _]](
      handlers: EndpointHandler[Op, F]*
  ): EndpointHandler[Op, F] =
    handlers.foldLeft[EndpointHandler[Op, F]](Combined(Vector()))(_ or _)

  private case class Combined[Op[_, _, _, _, _], F[_, _, _, _, _]](
      handlers: Vector[EndpointHandler[Op, F]]
  ) extends EndpointHandler[Op, F] {
    protected[smithy4s] def lift[Alg[_[_, _, _, _, _]]](
        service: Service.Aux[Alg, Op]
    ): PolyFunction5[Op, Kind5[F]#optional] =
      new PolyFunction5[Op, Kind5[F]#optional] {
        val lifted = handlers.map(_.lift(service))

        def apply[I, E, O, SI, SO](
            op: Op[I, E, O, SI, SO]
        ): Option[F[I, E, O, SI, SO]] = {
          var result: Option[F[I, E, O, SI, SO]] = None
          var i = 0
          while (result == None && i < lifted.size) {
            result = lifted(i)(op)
            i += 1
          }
          result
        }
      }
  }
}
