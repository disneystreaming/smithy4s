/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.kinds

object toPolyFunction5 {

  /**
    * Lifts a PolyFunction to a PolyFunction5
    */
  def apply[F[_], G[_]](
      f: PolyFunction[F, G]
  ): PolyFunction5[Kind1[F]#toKind5, Kind1[G]#toKind5] =
    new PolyFunction5[Kind1[F]#toKind5, Kind1[G]#toKind5] {
      def apply[I, E, O, SI, SO](fa: F[O]): G[O] = f(fa)
    }

  /**
    * Lifts a PolyFunction2 to a PolyFunction5
    */
  def apply[F[_, _], G[_, _]](
      f: PolyFunction2[F, G]
  ): PolyFunction5[Kind2[F]#toKind5, Kind2[G]#toKind5] =
    new PolyFunction5[Kind2[F]#toKind5, Kind2[G]#toKind5] {
      def apply[I, E, O, SI, SO](fa: F[E, O]): G[E, O] = f(fa)
    }

  def const5[F[_, _, _, _, _], G[-_, +_, +_, +_, +_]](
      value: G[Any, Nothing, Nothing, Nothing, Nothing]
  ): PolyFunction5[F, G] = new PolyFunction5[F, G] {
    def apply[I, E, O, SI, SO](f: F[I, E, O, SI, SO]): G[I, E, O, SI, SO] =
      value
  }

}
