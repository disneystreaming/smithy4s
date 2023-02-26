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

import kinds._

/**
  * Heterogenous function construct, allows to abstract over various kinds of functions
  * whilst providing an homogenous user experience without the user having to
  * manually lift functions from one kind to the other.
  *
  *{{{
  *  // assuming Foo is a code-generated interface
  *  val fooOption: Foo[Option] = ???
  *  val toList = new smithy4s.PolyFunction[Option, List]{def apply[A](fa: Option[A]): List[A] = fa.toList}
  *  val fooList: Foo[List] = foo.transform(toList)
  *}}}
  *
  * It is possible to plug arbitrary transformations to mechanism, such as `cats.arrow.FunctionK`
  */
trait Transformation[Func, Input, Output] {
  def apply(f: Func, input: Input): Output
}

object Transformation {
  def of[Input](input: Input): PartiallyApplied[Input] =
    new PartiallyApplied[Input](input)

  /**
    * A transformation that turns a monofunctor algebra into a bifunctor algebra by lifting the known errors in the
    * returned types of the operations of the algebra.
    */
  trait SurfaceError[F[_], G[_, _]] {
    def apply[E, A](fa: F[A], projectError: Throwable => Option[E]): G[E, A]
  }

  /**
    * A transformation that turns a bifunctor algebra into a monofunctor algebra by absorbing known errors in a
    * generic error channel that handles throwables.
    */
  trait AbsorbError[F[_, _], G[_]] {
    def apply[E, A](fa: F[E, A], injectError: E => Throwable): G[A]
  }

  /**
    * Partially applied transformation, can be used to create methods/extensions that allow for a reasonable UX.
    */
  class PartiallyApplied[Input](input: Input) {
    def apply[Func, Output](func: Func)(implicit
        transform: Transformation[Func, Input, Output]
    ) = transform(func, input)
  }

  // format: off
  implicit def functorK5_poly1_transformation[Alg[_[_, _, _, _, _]]: FunctorK5, F[_], G[_]]: Transformation[PolyFunction[F, G], FunctorAlgebra[Alg, F], FunctorAlgebra[Alg, G]] =
    new Transformation[PolyFunction[F, G], FunctorAlgebra[Alg, F], FunctorAlgebra[Alg, G]]{
      def apply(func: PolyFunction[F, G], algF: FunctorAlgebra[Alg, F]): FunctorAlgebra[Alg, G] = FunctorK5[Alg].mapK5[Kind1[F]#toKind5, Kind1[G]#toKind5](algF, toPolyFunction5(func))
    }

  implicit def functorK5_poly2_transformation[Alg[_[_, _, _, _, _]]: FunctorK5, F[_,_], G[_, _]]: Transformation[PolyFunction2[F, G], BiFunctorAlgebra[Alg, F], BiFunctorAlgebra[Alg, G]] =
    new Transformation[PolyFunction2[F, G], BiFunctorAlgebra[Alg, F], BiFunctorAlgebra[Alg, G]]{
      def apply(func: PolyFunction2[F, G], algF: BiFunctorAlgebra[Alg, F]): BiFunctorAlgebra[Alg, G] = FunctorK5[Alg].mapK5[Kind2[F]#toKind5, Kind2[G]#toKind5](algF, toPolyFunction5(func))
    }




  implicit def service_surfaceError_transformation[Alg[_[_, _, _, _, _]], F[_], G[_, _]](implicit service: Service[Alg]): Transformation[SurfaceError[F, G], FunctorAlgebra[Alg, F], BiFunctorAlgebra[Alg, G]] =
     new Transformation[SurfaceError[F, G], FunctorAlgebra[Alg, F], BiFunctorAlgebra[Alg, G]]{

       def apply(func: SurfaceError[F, G], algF: FunctorAlgebra[Alg, F]): BiFunctorAlgebra[Alg, G] = {
        val polyFunction = service.toPolyFunction[Kind1[F]#toKind5](algF)
        val interpreter = new PolyFunction5[service.Operation, Kind2[G]#toKind5]{
          def apply[I, E, O, SI, SO](op: service.Operation[I, E, O, SI, SO]): G[E,O] = {
            val endpoint = service.opToEndpoint(op)
            val catcher: Throwable => Option[E] = endpoint.errorable match {
              case None => PartialFunction.empty[Throwable, Option[E]]
              case Some(value) => value.liftError(_)
            }
            func.apply(polyFunction(op), catcher)
          }
        }
        service.fromPolyFunction[Kind2[G]#toKind5](interpreter)
      }
    }

  implicit def service_absorbError_transformation[Alg[_[_, _, _, _, _]], F[_, _], G[_]](implicit service: Service[Alg]): Transformation[AbsorbError[F, G], BiFunctorAlgebra[Alg, F], FunctorAlgebra[Alg, G]] =
     new Transformation[AbsorbError[F, G], BiFunctorAlgebra[Alg, F], FunctorAlgebra[Alg, G]]{

       def apply(func: AbsorbError[F, G], algF: BiFunctorAlgebra[Alg, F]): FunctorAlgebra[Alg, G] = {
        val polyFunction = service.toPolyFunction[Kind2[F]#toKind5](algF)
        val interpreter = new PolyFunction5[service.Operation, Kind1[G]#toKind5]{
          def apply[I, E, O, SI, SO](op: service.Operation[I, E, O, SI, SO]): G[O] = {
            val endpoint = service.opToEndpoint(op)
            val thrower: E => Throwable = endpoint.errorable match {
              case None =>
                // This case should not happen, as an endpoint without an errorable means the operation's error type is `Nothing`
                _ => new RuntimeException("Error coercion problem")
              case Some(value) => value.unliftError(_)
            }
            func.apply(polyFunction(op), thrower)
          }
        }
        service.fromPolyFunction[Kind1[G]#toKind5](interpreter)
      }
    }

  implicit def mappedHintsServiceTransform[Alg[_[_, _, _, _, _]]]: Transformation[Hints=>Hints, Service[Alg], Service[Alg]] =
    (mapper: Hints=>Hints, that: Service[Alg]) => new Service[Alg] {
      override type Operation[I, E, O, SI, SO] = that.Operation[I, E, O, SI, SO]

      private val cache = that.endpoints.foldLeft(Map.empty[ShapeId, Endpoint[I, E, O, SI, SO]]) {
        case (map, endpoint) => map.+(endpoint.id -> endpoint.mapHints(mapper))
      }

      override val endpoints: List[Endpoint[_, _, _, _, _]] = cache.values.toList

      override def endpoint[I, E, O, SI, SO](op: Operation[I, E, O, SI, SO]): (I, Endpoint[I, E, O, SI, SO]) = {
        that.endpoint(op) match {
          case (i, endpoint) => (i, cache(endpoint.id).asInstanceOf[Endpoint[I, E, O, SI, SO]])
        }
      }

      override def version: String = that.version

      override def hints: Hints = mapper(that.hints)

      override def reified: Alg[Operation] = that.reified

      def fromPolyFunction[P[_, _, _, _, _]](function: PolyFunction5[Operation, P]): Alg[P] = that.fromPolyFunction(function)

      def toPolyFunction[P[_, _, _, _, _]](algebra: Alg[P]): PolyFunction5[Operation, P] = that.toPolyFunction(algebra)

      override def id: ShapeId = that.id


      override def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: Alg[F], function: PolyFunction5[F, G]): Alg[G] = that.mapK5(alg, function)
    }


}
