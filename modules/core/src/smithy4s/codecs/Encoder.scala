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

import smithy4s.schema.CachedSchemaCompiler
import smithy4s.kinds.PolyFunction
import smithy4s.capability.EncoderK

/**
  * An abstraction that codifies the notion of transforming a piece of data into some output.
  */
trait Encoder[+Out, -A] { self =>

  /**
    * Symbolises the action of writing some content `A` into an output `Out`.
    */
  def encode(a: A): Out

  /**
    * Contramap the data which this writer works. The writer is a contravariant-functor on `A`.
    */
  final def contramap[B](f: B => A): Encoder[Out, B] =
    new Encoder[Out, B] {
      def encode(b: B): Out =
        self.encode(f(b))
    }

  /**
    * Transforms the Output type
    */
  final def andThen[Out0](
      f: Out => Out0
  ): Encoder[Out0, A] =
    new Encoder[Out0, A] {
      def encode(a: A): Out0 = f(self.encode(a))
    }

  /**
    * Connects this writer's output channel to the data channel of another writer. This is useful
    * for connecting an encoder into a larger writer.
    */
  def pipeToWriter[Message](
      writer: Writer[Message, Out]
  ): Writer[Message, A] =
    new Writer[Message, A] {
      def write(message: Message, a: A): Message =
        writer.write(message, self.encode(a))
    }

}

object Encoder {

  type CachedCompiler[Message] = CachedSchemaCompiler[Encoder[Message, *]]

  /**
    * Lifts an Output transformation as a higher-kinded function that
    * operates on encoders.
    */
  def andThenK[Out, Out0](
      f: Out => Out0
  ): PolyFunction[Encoder[Out, *], Encoder[Out0, *]] =
    new PolyFunction[Encoder[Out, *], Encoder[Out0, *]] {
      def apply[A](fa: Encoder[Out, A]): Encoder[Out0, A] =
        fa.andThen(f)
    }

  /**
    * Lifts an piping transformation that connects the result of an encoder
    * to the message channel of a writer.
    */
  def pipeToWriterK[Message, Out](
      other: Writer[Message, Out]
  ): PolyFunction[Encoder[Out, *], Writer[Message, *]] =
    new PolyFunction[Encoder[Out, *], Writer[Message, *]] {
      def apply[A](writer: Encoder[Out, A]): Writer[Message, A] =
        writer.pipeToWriter(other)
    }

  /**
    * Creates an encoder from a function.
    */
  def lift[A, Out](f: A => Out): Encoder[Out, A] = f(_)

  /**
    * Creates an encoder from a static output.
    */
  def static[Out](out: Out): Encoder[Out, Any] = _ => out

  implicit def encoderEncoderK[In, Out]: EncoderK[Encoder[Out, *], Out] =
    new EncoderK[Encoder[Out, *], Out] {
      def apply[A](fa: Encoder[Out, A], a: A): Out = fa.encode(a)
      def absorb[A](f: A => Out): Encoder[Out, A] = f(_)
    }

}
