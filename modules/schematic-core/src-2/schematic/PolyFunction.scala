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

package schematic

import schematic.maps.MMap

/**
  * Natural transformation, turning a polymorphic type into another,
  * whilst keeping the type parameter intact.
  */
trait PolyFunction[F[_], G[_]] { self =>
  def apply[A](fa: F[A]): G[A]

  /**
    * Creates a memoised version of this function.
    *
    * Unsafe because it creates mutable state, which is a
    * non-referentially-transparent action (aka a side-effect).
    */
  def unsafeMemoise: PolyFunction[Lambda[A => Static[F[A]]], G] =
    new PolyFunction[Lambda[A => Static[F[A]]], G] {
      private val map: MMap[F[_], Any] = MMap.empty

      def apply[A](static: Static[F[A]]): G[A] = {
        map
          .getOrElseUpdate(static, self(static))
          .asInstanceOf[G[A]]
      }

    }

  /**
   * Pre-computes the polyfunction by applying it on a vector
   * of possible inputs.
   *
   * Unsafe because calling the resulting polyfunction with an input that wasn't cached
   * will result in an exception.
   */
  def unsafeCache(
      allPossibleInputs: Vector[Existential[F]]
  ): PolyFunction[F, G] =
    new PolyFunction[F, G] {
      private val map: Map[Any, Any] = {
        val builder = Map.newBuilder[Any, Any]
        allPossibleInputs.foreach(input =>
          builder += input -> self
            .apply(input.asInstanceOf[F[Any]])
            .asInstanceOf[Any]
        )
        builder.result()
      }
      def apply[A](input: F[A]): G[A] = {
        map(input).asInstanceOf[G[A]]
      }
    }
}

object PolyFunction {

  def fromSchematic[S[_[_]], F[_]](
      schematic: S[F]
  ): PolyFunction[Lambda[A => Schema[S, A]], F] =
    new PolyFunction[Lambda[A => Schema[S, A]], F] {
      def apply[A](fa: Schema[S, A]): F[A] = fa.compile(schematic)
    }

}
