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

trait Reader[F[_], -Message, A] { self =>

  def read(message: Message): F[A]
  final def decode(message: Message): F[A] = read(message)

  final def mapK[G[_]](fk: PolyFunction[F, G]): Reader[G, Message, A] =
    new Reader[G, Message, A] {
      def read(message: Message): G[A] = fk(self.read(message))
    }

  final def compose[Message2](
      f: Message2 => Message
  ): Reader[F, Message2, A] =
    new Reader[F, Message2, A] {
      def read(message: Message2): F[A] = self.read(f(message))
    }

  final def map[B](f: A => B)(implicit C: Covariant[F]): Reader[F, Message, B] =
    new Reader[F, Message, B] {
      def read(message: Message): F[B] = C.map(self.read(message))(f)
    }

  final def narrow[M2 <: Message]: Reader[F, M2, A] =
    self.asInstanceOf[Reader[F, M2, A]]

  final def sequence(implicit Z: Zipper[F]): Reader[F, Seq[Message], Seq[A]] =
    new Reader[F, Seq[Message], Seq[A]] {
      def read(messages: Seq[Message]): F[Seq[A]] =
        Z.zipMapAll(messages.map(self.read).asInstanceOf[IndexedSeq[F[Any]]])(
          _.asInstanceOf[Seq[A]]
        )
    }

}

object Reader {

  implicit def readerZipper[F[_]: Zipper, Message]
      : Zipper[Reader[F, Message, *]] = new Zipper[Reader[F, Message, *]] {
    def pure[A](a: A): Reader[F, Message, A] = new Reader[F, Message, A] {
      def read(message: Message): F[A] = Zipper[F].pure(a)
    }

    def zipMapAll[A](seq: IndexedSeq[Reader[F, Message, Any]])(
        f: IndexedSeq[Any] => A
    ): Reader[F, Message, A] = new Reader[F, Message, A] {
      def read(message: Message): F[A] = {
        Zipper[F].zipMapAll(
          seq
            .asInstanceOf[IndexedSeq[Reader[F, Message, Any]]]
            .map(_.read(message))
        )(f)
      }
    }
  }

  def identity[F[_]: Zipper, A]: Reader[F, A, A] = new Reader[F, A, A] {
    def read(message: A): F[A] = Zipper[F].pure(message)
  }

  def decodeStatic[F[_], A](fa: F[A]): Reader[F, Any, A] =
    new Reader[F, Any, A] {
      def read(message: Any): F[A] = fa
    }

  def composeK[F[_], Message, Message2](
      f: Message2 => Message
  ): PolyFunction[Reader[F, Message, *], Reader[F, Message2, *]] =
    new PolyFunction[Reader[F, Message, *], Reader[F, Message2, *]] {
      def apply[A](fa: Reader[F, Message, A]): Reader[F, Message2, A] =
        fa.compose(f)
    }

  def liftPolyFunction[Message, F[_], G[_]](
      fk: PolyFunction[F, G]
  ): PolyFunction[Reader[F, Message, *], Reader[G, Message, *]] =
    new PolyFunction[Reader[F, Message, *], Reader[G, Message, *]] {
      def apply[A](fa: Reader[F, Message, A]): Reader[G, Message, A] =
        fa.mapK(fk)
    }

}
