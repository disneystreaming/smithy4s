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

package smithy4s.codecs

import smithy4s.kinds._
import smithy4s.capability.Covariant
import smithy4s.capability.Zipper

trait Decoder[F[_], -Message, A] { self =>

  def decode(message: Message): F[A]

  final def mapK[G[_]](fk: PolyFunction[F, G]): Decoder[G, Message, A] =
    new Decoder[G, Message, A] {
      def decode(message: Message): G[A] = fk(self.decode(message))
    }

  final def compose[Message2](
      f: Message2 => Message
  ): Decoder[F, Message2, A] =
    new Decoder[F, Message2, A] {
      def decode(message: Message2): F[A] = self.decode(f(message))
    }

  final def map[B](
      f: A => B
  )(implicit C: Covariant[F]): Decoder[F, Message, B] =
    new Decoder[F, Message, B] {
      def decode(message: Message): F[B] = C.map(self.decode(message))(f)
    }

  final def narrow[M2 <: Message]: Decoder[F, M2, A] =
    self.asInstanceOf[Decoder[F, M2, A]]

  final def sequence(implicit Z: Zipper[F]): Decoder[F, Seq[Message], Seq[A]] =
    new Decoder[F, Seq[Message], Seq[A]] {
      def decode(messages: Seq[Message]): F[Seq[A]] =
        Z.zipMapAll(messages.map(self.decode).asInstanceOf[IndexedSeq[F[Any]]])(
          _.asInstanceOf[Seq[A]]
        )
    }

}

object Decoder {

  def lift[F[_], Message, A](
      f: Message => F[A]
  ): Decoder[F, Message, A] = new Decoder[F, Message, A] {
    def decode(message: Message): F[A] = f(message)
  }

  def decodeStatic[F[_], A](fa: F[A]): Decoder[F, Any, A] =
    new Decoder[F, Any, A] {
      def decode(message: Any): F[A] = fa
    }

  def of[Message]: PartiallyAppliedDecoderBuilder[Message] =
    new PartiallyAppliedDecoderBuilder[Message]()

  class PartiallyAppliedDecoderBuilder[Message](
      private val dummy: Boolean = true
  ) extends AnyVal {
    def liftPolyFunction[F[_], G[_]](
        fk: PolyFunction[F, G]
    ): PolyFunction[Decoder[F, Message, *], Decoder[G, Message, *]] =
      new PolyFunction[Decoder[F, Message, *], Decoder[G, Message, *]] {
        def apply[A](fa: Decoder[F, Message, A]): Decoder[G, Message, A] =
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

  implicit def decoderZipper[F[_]: Zipper, Message]
      : Zipper[Decoder[F, Message, *]] = new Zipper[Decoder[F, Message, *]] {
    def pure[A](a: A): Decoder[F, Message, A] = new Decoder[F, Message, A] {
      def decode(message: Message): F[A] = Zipper[F].pure(a)
    }

    def zipMapAll[A](seq: IndexedSeq[Decoder[F, Message, Any]])(
        f: IndexedSeq[Any] => A
    ): Decoder[F, Message, A] = new Decoder[F, Message, A] {
      def decode(message: Message): F[A] = {
        Zipper[F].zipMapAll(
          seq
            .asInstanceOf[IndexedSeq[Decoder[F, Message, Any]]]
            .map(_.decode(message))
        )(f)
      }
    }
  }

}
