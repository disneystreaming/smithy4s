/*
 *  Copyright 2021 Disney Streaming
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

//format: off
/**
  * Generic representation of a service, as a list of "endpoints" (mapping to smithy operations).
  *
  * This abstraction lets us retrieve all information necessary to the generic implementation of
  * protocols, as well as transform implementations of finally-encoded interfaces into interpreters
  * (natural transformations) that operate on initially-encoded GADTs.
  *
  * @tparam Alg : a finally-encoded interface (commonly called algebra) that works
  *   against an abstract "effect" that takes 5 type parameters:
  *   Input, Error, Output, StreamedInput, StreamedOutput
  * @tparam Op : an initially encoded version of the finally-encoded interface. Typically,
  *   a GADT that acts as a reification of the operations. Passing the reified versions
  *   around makes it drastically easier to implement logic generically, without involving
  *   metaprogramming.
  */
trait Service[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]] extends Transformable[Alg] with HasId {
  implicit val selfService: Service[Alg, Op] = this

  def endpoints: List[Endpoint[Op, _, _, _, _, _]]
  def endpoint[I, E, O, SI, SO](op: Op[I, E, O, SI, SO]): (I, Endpoint[Op, I, E, O, SI, SO])
  val opToEndpoint : Transformation[Op, Endpoint[Op, *, *, *, *,*]] = new Transformation[Op, Endpoint[Op, *, *, *, *,*]]{
    def apply[I, E, O, SI, SO](op: Op[I,E,O,SI,SO]): Endpoint[Op,I,E,O,SI,SO] = endpoint(op)._2
  }

  def version: String

  def hints: Hints

  def transform[P[_, _, _, _, _]](transformation: Transformation[Op, P]): Alg[P]

  def asTransformation[F[_]](impl: Monadic[Alg, F]): Interpreter[Op, F] = asTransformationGen[GenLift[F]#λ](impl)

  def asTransformationGen[P[_, _, _, _, _]](impl: Alg[P]): Transformation[Op, P]

  // Apply a transformation that is aware of the endpoint
  def transformWithEndpoint[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: Alg[P], transformation: Transformation.ZippedWithEndpoint[P, Op, P1]) : Alg[P1] = {
    this.transform(asTransformationGen(alg).zip(opToEndpoint).andThen(transformation))
  }
}
