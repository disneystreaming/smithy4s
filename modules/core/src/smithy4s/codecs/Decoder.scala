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

package smithy4s.codecs

import smithy4s.kinds._
import smithy4s.capability.Covariant
import smithy4s.capability.Zipper

/**
  * An abstraction that codifies the action of reading data from some input.
  */
trait Decoder[F[_], -In, A] { self =>

  def decode(in: In): F[A]

  final def mapK[G[_]](fk: PolyFunction[F, G]): Decoder[G, In, A] =
    new Decoder[G, In, A] {
      def decode(in: In): G[A] = fk(self.decode(in))
    }

  final def compose[In2](
      f: In2 => In
  ): Decoder[F, In2, A] =
    new Decoder[F, In2, A] {
      def decode(in: In2): F[A] = self.decode(f(in))
    }

  final def map[B](
      f: A => B
  )(implicit C: Covariant[F]): Decoder[F, In, B] =
    new Decoder[F, In, B] {
      def decode(in: In): F[B] = C.map(self.decode(in))(f)
    }

  final def narrow[M2 <: In]: Decoder[F, M2, A] =
    self.asInstanceOf[Decoder[F, M2, A]]

  final def sequence(implicit Z: Zipper[F]): Decoder[F, Seq[In], Seq[A]] =
    new Decoder[F, Seq[In], Seq[A]] {
      def decode(ins: Seq[In]): F[Seq[A]] =
        Z.zipMapAll(ins.map(self.decode).asInstanceOf[IndexedSeq[F[Any]]])(
          _.asInstanceOf[Seq[A]]
        )
    }

}

object Decoder {

  def lift[F[_], In, A](
      f: In => F[A]
  ): Decoder[F, In, A] = new Decoder[F, In, A] {
    def decode(in: In): F[A] = f(in)
  }

  def static[F[_], A](fa: F[A]): Decoder[F, Any, A] =
    new Decoder[F, Any, A] {
      def decode(in: Any): F[A] = fa
    }

  def of[In]: PartiallyAppliedDecoderBuilder[In] =
    new PartiallyAppliedDecoderBuilder[In]()

  class PartiallyAppliedDecoderBuilder[In](
      private val dummy: Boolean = true
  ) extends AnyVal {
    def liftPolyFunction[F[_], G[_]](
        fk: PolyFunction[F, G]
    ): PolyFunction[Decoder[F, In, *], Decoder[G, In, *]] =
      new PolyFunction[Decoder[F, In, *], Decoder[G, In, *]] {
        def apply[A](fa: Decoder[F, In, A]): Decoder[G, In, A] =
          fa.mapK(fk)
      }
  }

  def in[F[_]]: PartiallyAppliedDecoderBuilderF[F] =
    new PartiallyAppliedDecoderBuilderF[F]

  class PartiallyAppliedDecoderBuilderF[F[_]](private val dummy: Boolean = true)
      extends AnyVal {
    def composeK[To, From](
        f: From => To
    ): PolyFunction[Decoder[F, To, *], Decoder[F, From, *]] =
      new PolyFunction[Decoder[F, To, *], Decoder[F, From, *]] {
        def apply[A](fa: Decoder[F, To, A]): Decoder[F, From, A] =
          fa.compose(f)
      }

  }

  implicit def decoderZipper[F[_]: Zipper, In]: Zipper[Decoder[F, In, *]] =
    new Zipper[Decoder[F, In, *]] {
      def pure[A](a: A): Decoder[F, In, A] = new Decoder[F, In, A] {
        def decode(in: In): F[A] = Zipper[F].pure(a)
      }

      def zipMapAll[A](seq: IndexedSeq[Decoder[F, In, Any]])(
          f: IndexedSeq[Any] => A
      ): Decoder[F, In, A] = new Decoder[F, In, A] {
        def decode(in: In): F[A] = {
          Zipper[F].zipMapAll(
            seq
              .asInstanceOf[IndexedSeq[Decoder[F, In, Any]]]
              .map(_.decode(in))
          )(f)
        }
      }
    }

}
