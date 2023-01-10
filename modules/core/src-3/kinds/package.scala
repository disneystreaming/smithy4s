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

package object kinds {

  // format: off
  // Not using type projectors as it messes with type inference when using the algebra
  type FunctorAlgebra[Alg[_[_, _, _, _, _]], F[_]] = Alg[[I, E, O, SI, SO] =>> F[O]]
  type FunctorInterpreter[Op[_, _, _, _, _], F[_]] = PolyFunction5[Op, [I, E, O, SI, SO] =>> F[O]]
  type BiFunctorAlgebra[Alg[_[_, _, _, _, _]], F[_, _]] = Alg[[I, E, O, SI, SO] =>> F[E, O]]
  type BiFunctorInterpreter[Op[_, _, _, _, _], F[_, _]] = PolyFunction5[Op, [I, E, O, SI, SO] =>> F[E, O]]
  // format: on

  type Kind1[F[_]] = {
    type toKind2[E, O] = F[O]
    type toKind5[I, E, O, SI, SO] = F[O]
  }

  type Kind2[F[_, _]] = {
    type toKind5[I, E, O, SI, SO] = F[E, O]
  }

}
