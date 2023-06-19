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

import smithy4s.kinds.PolyFunction
import smithy4s.capability.EncoderK

/**
  * An abstraction that codifies the notion of writing a piece of data into a message.
  *
  * This is particularly useful for http requests/responses, where different subsets of data
  * have different impacts different locations of the http message : some fields may
  * impact headers, some fields may impact the http body, other things that are driven from
  * static information (smithy traits) may lead to a transformation of the message ...
  *
  * Having the ability to decompose the notion of encoding a piece of data into different
  * writers that can be composed together is powerful and helps centralising some complexity
  * in third-party agnostic code.
  */
trait Writer[Message, A] { self =>

  /**
    * Symbolises the action of writing some content A into a message, which returns
    * an update message.
    */
  def write(message: Message, a: A): Message

  final def contramap[B](f: B => A): Writer[Message, B] =
    new Writer[Message, B] {
      def write(message: Message, b: B): Message =
        self.write(message: Message, f(b))
    }

  final def andThen(f: Message => Message): Writer[Message, A] =
    new Writer[Message, A] {
      def write(message: Message, a: A): Message = f(self.write(message, a))
    }

  final def compose(f: Message => Message): Writer[Message, A] =
    new Writer[Message, A] {
      def write(message: Message, a: A): Message = self.write(f(message), a)
    }

  def combine(other: Writer[Message, A]): Writer[Message, A] =
    new Writer[Message, A] {
      def write(message: Message, a: A): Message =
        other.write(self.write(message, a), a)
    }

}

object Writer {

  def noop[Message, A]: Writer[Message, A] = new Writer[Message, A] {
    def write(message: Message, a: A): Message = message
  }

  def andThenK[Message](
      f: Message => Message
  ): PolyFunction[Writer[Message, *], Writer[Message, *]] =
    new PolyFunction[Writer[Message, *], Writer[Message, *]] {
      def apply[A](fa: Writer[Message, A]): Writer[Message, A] =
        fa.andThen(f)
    }

  def composeK[Message](
      f: Message => Message
  ): PolyFunction[Writer[Message, *], Writer[Message, *]] =
    new PolyFunction[Writer[Message, *], Writer[Message, *]] {
      def apply[A](fa: Writer[Message, A]): Writer[Message, A] =
        fa.compose(f)
    }

  // format: off
  implicit def writerEncoderK[Message]: EncoderK[Writer[Message, *], Message => Message] =
    new EncoderK[Writer[Message, *], Message => Message] {
      def apply[A](fa: Writer[Message, A], a: A): Message => Message = fa.write(_, a)
      def absorb[A](f: A => (Message => Message)): Writer[Message, A] = new Writer[Message, A] {
        def write(message: Message, a: A): Message = f(a)(message)
      }
    }
  // format: on

}
