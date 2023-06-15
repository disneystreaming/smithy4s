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

package smithy4s.capability

import smithy4s.kinds.PolyFunction

trait Encoder[Message, A] { self =>

  def encode(message: Message, a: A): Message

  final def contramap[B](f: B => A): Encoder[Message, B] =
    new Encoder[Message, B] {
      def encode(message: Message, b: B): Message =
        self.encode(message: Message, f(b))
    }

  final def andThen(f: Message => Message): Encoder[Message, A] =
    new Encoder[Message, A] {
      def encode(message: Message, a: A): Message = f(self.encode(message, a))
    }

  final def compose(f: Message => Message): Encoder[Message, A] =
    new Encoder[Message, A] {
      def encode(message: Message, a: A): Message = self.encode(f(message), a)
    }

  def combine(other: Encoder[Message, A]): Encoder[Message, A] =
    new Encoder[Message, A] {
      def encode(message: Message, a: A): Message =
        other.encode(self.encode(message, a), a)
    }

}

object Encoder {

  def noop[Message, A]: Encoder[Message, A] = new Encoder[Message, A] {
    def encode(message: Message, a: A): Message = message
  }

  def andThenK[Message](
      f: Message => Message
  ): PolyFunction[Encoder[Message, *], Encoder[Message, *]] =
    new PolyFunction[Encoder[Message, *], Encoder[Message, *]] {
      def apply[A](fa: Encoder[Message, A]): Encoder[Message, A] =
        fa.andThen(f)
    }

  def composeK[Message](
      f: Message => Message
  ): PolyFunction[Encoder[Message, *], Encoder[Message, *]] =
    new PolyFunction[Encoder[Message, *], Encoder[Message, *]] {
      def apply[A](fa: Encoder[Message, A]): Encoder[Message, A] =
        fa.compose(f)
    }

  // format: off
  implicit def encoderEncoderK[Message]: EncoderK[Encoder[Message, *], Message => Message] =
    new EncoderK[Encoder[Message, *], Message => Message] {
      def apply[A](fa: Encoder[Message, A], a: A): Message => Message = fa.encode(_, a)
      def absorb[A](f: A => (Message => Message)): Encoder[Message, A] = new Encoder[Message, A] {
        def encode(message: Message, a: A): Message = f(a)(message)
      }
    }
  // format: on

}
