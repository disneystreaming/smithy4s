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

import Transformation.Zip

/**
  * Natural transformation allowing to change the outer type
  * that final interfaces work against.
  */
trait Transformation[F[_, _, _, _, _], G[_, _, _, _, _]] { self =>
  def apply[I, E, O, SI, SO](fa: F[I, E, O, SI, SO]): G[I, E, O, SI, SO]

  def zip[G2[_, _, _, _, _]](
      other: Transformation[F, G2]
  ): Transformation[F, Zip[G, G2, *, *, *, *, *]] =
    new Transformation[F, Zip[G, G2, *, *, *, *, *]] {
      def apply[I, E, O, SI, SO](
          fa: F[I, E, O, SI, SO]
      ): Zip[G, G2, I, E, O, SI, SO] =
        Zip(self(fa), other(fa))
    }

  def andThen[H[_, _, _, _, _]](
      other: Transformation[G, H]
  ): Transformation[F, H] = new Transformation[F, H] {
    def apply[I, E, O, SI, SO](fa: F[I, E, O, SI, SO]): H[I, E, O, SI, SO] =
      other(self(fa))
  }

  /**
   * Pre-computes the transormation by applying it on an iterable of all possible inputs.
   *
   * Unsafe because calling the resulting transformation with an value that wasn't precomputed
   * will result in an exception.
   *
  * See https://stackoverflow.com/questions/67750145/how-to-implement-types-like-mapk-in-scala-3-dotty
   */
  def precompute(
      allPossibleInputs: Iterable[Kind5.Existential[F]]
  ): Transformation[F, G] =
    new Transformation[F, G] {
      private val map: Map[Any, Any] = {
        val builder = Map.newBuilder[Any, Any]
        allPossibleInputs.foreach(input =>
          builder += input -> self
            .apply(input.asInstanceOf[F[Any, Any, Any, Any, Any]])
            .asInstanceOf[Any]
        )
        builder.result()
      }
      def apply[I, E, O, SI, SO](
          input: F[I, E, O, SI, SO]
      ): G[I, E, O, SI, SO] = {
        map(input).asInstanceOf[G[I, E, O, SI, SO]]
      }
    }

}

object Transformation {

  def identity[F[_, _, _, _, _]]: Transformation[F, F] =
    new Transformation[F, F] {
      def apply[I, E, O, SI, SO](fa: F[I, E, O, SI, SO]): F[I, E, O, SI, SO] =
        fa
    }

  // format: off
  case class Zip[F1[_, _, _, _, _], F2[_, _, _, _, _], I, E, O, SI, SO](
      _1: F1[I, E, O, SI, SO],
      _2: F2[I, E, O, SI, SO]
  )

  object Zip {
    type Of[F1[_, _, _, _, _], F2[_, _, _, _, _]] = {
      type λ[I, E, O, SI, SO] = Zip[F1, F2, I, E, O, SI, SO]
    }
  }

  type Zipped[F1[_, _, _, _, _], F2[_, _, _, _, _], G[_, _, _, _, _]] =
    Transformation[Zip.Of[F1, F2]#λ, G]

  type ZippedWithEndpoint[F[_, _, _, _, _], Op[_,_,_,_,_], G[_,_,_,_,_]] =
    Zipped[F, Endpoint[Op, *, *, *, *, *], G]
  // format: on

}
