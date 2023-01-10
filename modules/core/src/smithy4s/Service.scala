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

//format: off
/**
  * Generic representation of a service, as a list of "endpoints" (mapping to smithy operations).
  *
  * This abstraction lets us retrieve all information necessary to the generic implementation of
  * protocols, as well as transform implementations of finally-encoded interfaces into interpreters
  * (polymorphic functions) that operate on initially-encoded GADTs.
  *
  * @tparam Alg: a finally-encoded interface (commonly called algebra) that works
  *   against an abstract "effect" that takes 5 type parameters:
  *   Input, Error, Output, StreamedInput, StreamedOutput
  * @tparam Op: an initially encoded version of the finally-encoded interface. Typically,
  *   a GADT that acts as a reification of the operations. Passing the reified versions
  *   around makes it drastically easier to implement logic generically, without involving
  *   metaprogramming.
  */
trait Service[Alg[_[_, _, _, _, _]]] extends FunctorK5[Alg] with HasId {
  type Operation[I, E, O, SI, SO]
  type Endpoint[I, E, O, SI, SO] = smithy4s.Endpoint[Operation, I, E, O, SI, SO]
  type Interpreter[F[_, _, _, _, _]] = PolyFunction5[Operation, F]
  type FunctorInterpreter[F[_]] = PolyFunction5[Operation, kinds.Kind1[F]#toKind5]
  type BiFunctorInterpreter[F[_, _]] = PolyFunction5[Operation, kinds.Kind2[F]#toKind5]
  type Impl[F[_]] = Alg[kinds.Kind1[F]#toKind5]
  type ErrorAware[F[_, _]] = Alg[kinds.Kind2[F]#toKind5]

  val service: Service[Alg] = this
  def endpoints: List[Endpoint[_, _, _, _, _]]
  def endpoint[I, E, O, SI, SO](op: Operation[I, E, O, SI, SO]): (I, Endpoint[I, E, O, SI, SO])
  def version: String
  def hints: Hints
  def reified: Alg[Operation]
  def fromPolyFunction[P[_, _, _, _, _]](function: PolyFunction5[Operation, P]): Alg[P]
  def toPolyFunction[P[_, _, _, _, _]](algebra: Alg[P]): PolyFunction5[Operation, P]

  final val opToEndpoint: PolyFunction5[Operation, Endpoint] = new PolyFunction5[Operation, Endpoint]{
    def apply[I, E, O, SI, SO](op: Operation[I,E,O,SI,SO]): Endpoint[I,E,O,SI,SO] = endpoint(op)._2
  }

}

object Service {

  def apply[Alg[_[_, _, _, _, _]]](implicit ev: Service[Alg]): ev.type = ev

  type Aux[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]] = Service[Alg]{ type Operation[I, E, O, SI, SO] = Op[I, E, O, SI, SO] }

  trait Mixin[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]] extends Service[Alg]{
    implicit val serviceInstance: Service[Alg] = this
    type Operation[I, E, O, SI, SO] = Op[I, E, O, SI, SO]
  }

  /**
    * A Service the algebra of which is a PolyFunction
    */
  trait Reflective[Op[_, _, _, _, _]] extends Service[PolyFunction5.From[Op]#Algebra] {
    type Operation[I, E, O, SI, SO] = Op[I, E, O, SI, SO]
    final def reified: PolyFunction5[Op, Op] = PolyFunction5.identity
    final def fromPolyFunction[P[_, _, _, _, _]](function: PolyFunction5[Op, P]): PolyFunction5[Op, P] = function
    final def toPolyFunction[P[_, _, _, _, _]](algebra: PolyFunction5[Op, P]): PolyFunction5[Op, P] = algebra
    final def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](algebra: PolyFunction5[Op, F], function: PolyFunction5[F, G]): PolyFunction5[Op, G] = algebra.andThen(function)
  }
}
