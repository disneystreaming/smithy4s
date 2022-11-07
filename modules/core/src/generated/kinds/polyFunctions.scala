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

/////// THIS FILE WAS GENERATED AT BUILD TIME, AND CHECKED-IN FOR DISCOVERABILITY ///////

package smithy4s
package kinds

import smithy4s.capability._

trait PolyFunction[F[_], G[_]] { self =>
  def apply[A0](fa: F[A0]): G[A0]

  final def andThen[H[_]](other: PolyFunction[G, H]): PolyFunction[F, H] =
    new PolyFunction[F, H] {
      def apply[A0](fa: F[A0]): H[A0] = other(self(fa))
    }

  import Kind1._
  private[smithy4s] final def unsafeCacheBy[K](
      allPossibleInputs: Seq[Existential[F]],
      getKey: Existential[F] => K
  ): PolyFunction[F, G] =
    new PolyFunction[F, G] {
      private val map: Map[K, Any] = {
        val builder = Map.newBuilder[K, Any]
        allPossibleInputs.foreach(input =>
          builder += getKey(input) -> self
            .apply(input.asInstanceOf[F[Any]])
            .asInstanceOf[Any]
        )
        builder.result()
      }
      def apply[A0](input: F[A0]): G[A0] =
        map(getKey(existential(input))).asInstanceOf[G[A0]]
    }
}
object PolyFunction {
  type From[F[_]] = {
    type Algebra[G[_]] = PolyFunction[F, G]
  }

  def identity[F[_]]: PolyFunction[F, F] = new PolyFunction[F, F] {
    def apply[A0](input: F[A0]): F[A0] = input
  }

  implicit def polyfunction_transformation[Alg[_[_]]: FunctorK, F[_], G[_]]
      : Transformation[PolyFunction[F, G], Alg[F], Alg[G]] =
    new Transformation[PolyFunction[F, G], Alg[F], Alg[G]] {
      def apply(func: PolyFunction[F, G], algF: Alg[F]): Alg[G] =
        FunctorK[Alg].mapK(algF, func)
    }
}

trait PolyFunction2[F[_, _], G[_, _]] { self =>
  def apply[A0, A1](fa: F[A0, A1]): G[A0, A1]

  final def andThen[H[_, _]](other: PolyFunction2[G, H]): PolyFunction2[F, H] =
    new PolyFunction2[F, H] {
      def apply[A0, A1](fa: F[A0, A1]): H[A0, A1] = other(self(fa))
    }

  import Kind2._
  private[smithy4s] final def unsafeCacheBy[K](
      allPossibleInputs: Seq[Existential[F]],
      getKey: Existential[F] => K
  ): PolyFunction2[F, G] =
    new PolyFunction2[F, G] {
      private val map: Map[K, Any] = {
        val builder = Map.newBuilder[K, Any]
        allPossibleInputs.foreach(input =>
          builder += getKey(input) -> self
            .apply(input.asInstanceOf[F[Any, Any]])
            .asInstanceOf[Any]
        )
        builder.result()
      }
      def apply[A0, A1](input: F[A0, A1]): G[A0, A1] = map(
        getKey(existential(input))
      ).asInstanceOf[G[A0, A1]]
    }
}
object PolyFunction2 {
  type From[F[_, _]] = {
    type Algebra[G[_, _]] = PolyFunction2[F, G]
  }

  def identity[F[_, _]]: PolyFunction2[F, F] = new PolyFunction2[F, F] {
    def apply[A0, A1](input: F[A0, A1]): F[A0, A1] = input
  }

  implicit def polyfunction2_transformation[Alg[_[_, _]]: FunctorK2, F[_, _], G[
      _,
      _
  ]]: Transformation[PolyFunction2[F, G], Alg[F], Alg[G]] =
    new Transformation[PolyFunction2[F, G], Alg[F], Alg[G]] {
      def apply(func: PolyFunction2[F, G], algF: Alg[F]): Alg[G] =
        FunctorK2[Alg].mapK2(algF, func)
    }
}

trait PolyFunction5[F[_, _, _, _, _], G[_, _, _, _, _]] { self =>
  def apply[A0, A1, A2, A3, A4](
      fa: F[A0, A1, A2, A3, A4]
  ): G[A0, A1, A2, A3, A4]

  final def andThen[H[_, _, _, _, _]](
      other: PolyFunction5[G, H]
  ): PolyFunction5[F, H] = new PolyFunction5[F, H] {
    def apply[A0, A1, A2, A3, A4](
        fa: F[A0, A1, A2, A3, A4]
    ): H[A0, A1, A2, A3, A4] = other(self(fa))
  }

  import Kind5._
  private[smithy4s] final def unsafeCacheBy[K](
      allPossibleInputs: Seq[Existential[F]],
      getKey: Existential[F] => K
  ): PolyFunction5[F, G] =
    new PolyFunction5[F, G] {
      private val map: Map[K, Any] = {
        val builder = Map.newBuilder[K, Any]
        allPossibleInputs.foreach(input =>
          builder += getKey(input) -> self
            .apply(input.asInstanceOf[F[Any, Any, Any, Any, Any]])
            .asInstanceOf[Any]
        )
        builder.result()
      }
      def apply[A0, A1, A2, A3, A4](
          input: F[A0, A1, A2, A3, A4]
      ): G[A0, A1, A2, A3, A4] =
        map(getKey(existential(input))).asInstanceOf[G[A0, A1, A2, A3, A4]]
    }
}
object PolyFunction5 {
  type From[F[_, _, _, _, _]] = {
    type Algebra[G[_, _, _, _, _]] = PolyFunction5[F, G]
  }

  def identity[F[_, _, _, _, _]]: PolyFunction5[F, F] =
    new PolyFunction5[F, F] {
      def apply[A0, A1, A2, A3, A4](
          input: F[A0, A1, A2, A3, A4]
      ): F[A0, A1, A2, A3, A4] = input
    }

  implicit def polyfunction5_transformation[Alg[_[_, _, _, _, _]]: FunctorK5, F[
      _,
      _,
      _,
      _,
      _
  ], G[_, _, _, _, _]]: Transformation[PolyFunction5[F, G], Alg[F], Alg[G]] =
    new Transformation[PolyFunction5[F, G], Alg[F], Alg[G]] {
      def apply(func: PolyFunction5[F, G], algF: Alg[F]): Alg[G] =
        FunctorK5[Alg].mapK5(algF, func)
    }
}
