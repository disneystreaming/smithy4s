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

import smithy4s.schema._
import smithy4s.kinds.PolyFunction
import smithy4s.capability.EncoderK

/**
  * An abstraction that codifies the notion of writing a piece of data into a message.
  *
  * This is particularly useful for http requests/responses, where different subsets of data
  * have a different impact on different locations of the http message : some fields may
  * impact headers, some fields may impact the http body, other things that are driven from
  * static information (smithy traits) may lead to a transformation of the message ...
  *
  * Having the ability to decompose the notion of encoding a piece of data into different
  * writers that can be composed together is powerful and helps centralising some complexity
  * in third-party agnostic code.
  */
trait Writer[-In, +Out, -A] { self =>

  /**
    * Symbolises the action of writing some content A into an input message, which returns
    * an output message.
    */
  def write(message: In, a: A): Out

  def encode[In0 <: In](a: A)(implicit ev: Unit =:= In0): Out =
    write(ev.apply(()), a)

  final def contramap[B](f: B => A): Writer[In, Out, B] =
    new Writer[In, Out, B] {
      def write(message: In, b: B): Out =
        self.write(message: In, f(b))
    }

  final def andThen[Out0](
      f: Out => Out0
  ): Writer[In, Out0, A] =
    new Writer[In, Out0, A] {
      def write(message: In, a: A): Out0 = f(self.write(message, a))
    }

  final def compose[In0](
      f: In0 => In
  ): Writer[In0, Out, A] =
    new Writer[In0, Out, A] {
      def write(message: In0, a: A): Out = self.write(f(message), a)
    }

  def pipe[Out0, A0 <: A](other: Writer[Out, Out0, A0]): Writer[In, Out0, A0] =
    new Writer[In, Out0, A0] {
      def write(message: In, a: A0): Out0 =
        other.write(self.write(message, a), a)
    }

}

object Writer {

  type CachedCompiler[In, Out] = CachedSchemaCompiler[Writer[In, Out, *]]

  def encodeBy[A, Message](f: A => Message): Writer[Unit, Message, A] =
    new Writer[Unit, Message, A] {
      def write(message: Unit, a: A): Message = f(a)
    }

  def encodeStatic[Message, A](message: Message): Writer[Unit, Message, A] =
    new Writer[Unit, Message, A] {
      def write(unit: Unit, a: A): Message = message
    }

  def noop[Message, A]: Writer[Message, Message, A] =
    new Writer[Message, Message, A] {
      def write(message: Message, a: A): Message = message
    }

  def andThenK[In, Out, Out0](
      f: Out => Out0
  ): PolyFunction[Writer[In, Out, *], Writer[In, Out0, *]] =
    new PolyFunction[Writer[In, Out, *], Writer[In, Out0, *]] {
      def apply[A](fa: Writer[In, Out, A]): Writer[In, Out0, A] =
        fa.andThen(f)
    }

  def andThenK_[Message](
      f: Message => Message
  ): PolyFunction[Writer[Message, Message, *], Writer[Message, Message, *]] =
    andThenK(f)

  def composeK[In0, In, Out](
      f: In0 => In
  ): PolyFunction[Writer[In, Out, *], Writer[In0, Out, *]] =
    new PolyFunction[Writer[In, Out, *], Writer[In0, Out, *]] {
      def apply[A](fa: Writer[In, Out, A]): Writer[In0, Out, A] =
        fa.compose(f)
    }

  def composeK_[Message](
      f: Message => Message
  ): PolyFunction[Writer[Message, Message, *], Writer[Message, Message, *]] =
    composeK(f)

  // format: off
  implicit def writerEncoderK[In, Out]: EncoderK[Writer[In, Out, *], In => Out] =
    new EncoderK[Writer[In, Out, *], In => Out] {
      def apply[A](fa: Writer[In, Out, A], a: A): In => Out = fa.write(_, a)
      def absorb[A](f: A => (In => Out)): Writer[In, Out, A] = new Writer[In, Out, A] {
        def write(message: In, a: A): Out = f(a)(message)
      }
    }
  // format: on

}
