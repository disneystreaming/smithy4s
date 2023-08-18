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
  * An abstraction that codifies the notion of writing a piece of data into an output,
  * provided some contextual information.
  *
  * This has two input channels:
  *   * one for contextual information (In)
  *   * one for the actual data (A)
  *
  * This is particularly useful for http requests/responses, where different subsets of data
  * have a different impact on different locations of the http message : some fields may
  * impact headers, some fields may impact the http body, other things that are driven from
  * static information (smithy traits) may lead to a transformation of the message. In this situation,
  * the Input and Output channels are of the same type.
  *
  * Having the ability to decompose the notion of encoding a piece of data into different
  * writers that can be composed together is powerful and helps centralising some complexity
  * in third-party agnostic code.
  *
  * @tparam In : some input channel used as context to write data. When set to Any, the implication
  *   is that the data produces some output on its own
  * @tparam Out: the output channel in which the data is written.
  * @tparam A: the type of data that is being written into the output channel
  */
// scalafmt: {maxColumn = 120}
trait Writer[-In, +Out, -A] { self =>

  /**
    * Symbolises the action of writing some content `A` into an output `Out`, provided some context `In`
    */
  def write(input: In, a: A): Out

  /**
    * When the data `A` is sufficient to produce the output, anything can be used as a context input.
    * Therefore, traditional encoders (like Json/Xml) can be modelled as Writers with type `In == Any`
    *
    * This method is a short-hand for encoding the data without the caller forced to pass a dummy input.
    */
  def encode[In0 <: In](a: A)(implicit ev: Any =:= In0): Out =
    write(ev.apply(()), a)

  /**
    * Contramap the data which this writer works. The writer is a contravariant-functor on `A`.
    */
  final def contramap[B](f: B => A): Writer[In, Out, B] =
    new Writer[In, Out, B] {
      def write(message: In, b: B): Out =
        self.write(message: In, f(b))
    }

  /**
    * Transforms the Output type
    */
  final def andThen[Out0](
      f: Out => Out0
  ): Writer[In, Out0, A] =
    new Writer[In, Out0, A] {
      def write(message: In, a: A): Out0 = f(self.write(message, a))
    }

  /**
    * Transforms the context Input type
    */
  final def compose[In0](
      f: In0 => In
  ): Writer[In0, Out, A] =
    new Writer[In0, Out, A] {
      def write(message: In0, a: A): Out = self.write(f(message), a)
    }

  /**
    * Connects this writer's output channel to the contextual input channel of another writer.
    */
  def pipe[Out0, A0 <: A](other: Writer[Out, Out0, A0]): Writer[In, Out0, A0] =
    new Writer[In, Out0, A0] {
      def write(message: In, a: A0): Out0 =
        other.write(self.write(message, a), a)
    }

  /**
    * Connects this writer's output channel to the data channel of another writer. This is useful
    * for connecting an encoder into a larger writer.
    */
  def pipeData[Message <: In, Out0 >: Out](other: Writer[Message, Message, Out]): Writer[Message, Message, A] =
    new Writer[Message, Message, A] {
      def write(message: Message, a: A): Message = other.write(message, self.write(message, a))
    }

}

object Writer {

  type CachedCompiler[In, Out] = CachedSchemaCompiler[Writer[In, Out, *]]

  def addingTo[In]: PartiallyAppliedWriterBuilder[In] = new PartiallyAppliedWriterBuilder()

  class PartiallyAppliedWriterBuilder[In](val dummy: Boolean = true) extends AnyVal {

    /**
    * Lifts an Output transformation as a higher-kinded function that
    * operates on writers.
    */
    def andThenK[Out, Out0](
        f: Out => Out0
    ): PolyFunction[Writer[In, Out, *], Writer[In, Out0, *]] =
      new PolyFunction[Writer[In, Out, *], Writer[In, Out0, *]] {
        def apply[A](fa: Writer[In, Out, A]): Writer[In, Out0, A] =
          fa.andThen(f)
      }

    /**
    * Lifts an Output transformation as a higher-kinded function that
    * operates on writers.
    */
    def andThenK_(f: In => In): PolyFunction[Writer[In, In, *], Writer[In, In, *]] =
      andThenK(f)

    /**
    * Lifts an piping transformation that connects the output channel of a writer
    * to the data channel of another writer.
    */
    def pipeDataK[Out](
        other: Writer[In, In, Out]
    ): PolyFunction[Writer[In, Out, *], Writer[In, In, *]] =
      new PolyFunction[Writer[In, Out, *], Writer[In, In, *]] {
        def apply[A](writer: Writer[In, Out, A]): Writer[In, In, A] =
          writer.pipeData(other)
      }
  }

  /**
    * Creates an writer from a function.
    */
  def lift[In, Message, A](f: (In, A) => Message): Writer[In, Message, A] = { (in, a) => f(in, a) }

  /**
    * Creates an encoder (a writer that takes any input) from a function.
    */
  def encodeBy[A, Message](f: A => Message): Encoder[Message, A] = { (_, a) => f(a) }

  /**
    * Creates an encoder (a writer that takes any input) from a static output.
    */
  def encodeStatic[Message](message: Message): Encoder[Message, Any] =
    new Encoder[Message, Any] {
      def write(in: Any, a: Any): Message = message
    }

  /**
    * Creates a writer that returns its input as its output, without taking
    * the data into consideration
    */
  def noop[Message]: Writer[Message, Message, Any] =
    new Writer[Message, Message, Any] {
      def write(message: Message, a: Any): Message = message
    }

  /**
    * Lifts an Output transformation as a higher-kinded function that
    * operates on writers.
    */
  def andThenK[In, Out, Out0](
      f: Out => Out0
  ): PolyFunction[Writer[In, Out, *], Writer[In, Out0, *]] =
    new PolyFunction[Writer[In, Out, *], Writer[In, Out0, *]] {
      def apply[A](fa: Writer[In, Out, A]): Writer[In, Out0, A] =
        fa.andThen(f)
    }

  /**
    * Lifts an Output transformation as a higher-kinded function that
    * operates on writers.
    */
  def andThenK_[Message](
      f: Message => Message
  ): PolyFunction[Writer[Message, Message, *], Writer[Message, Message, *]] =
    andThenK(f)

  /**
    * Lifts an Input transformation as a higher-kinded function that
    * operates on writers.
    */
  def composeK[In0, In, Out](
      f: In0 => In
  ): PolyFunction[Writer[In, Out, *], Writer[In0, Out, *]] =
    new PolyFunction[Writer[In, Out, *], Writer[In0, Out, *]] {
      def apply[A](fa: Writer[In, Out, A]): Writer[In0, Out, A] =
        fa.compose(f)
    }

  /**
    * Lifts an Input transformation as a higher-kinded function that
    * operates on writers, when the Input and Output are of the same type.
    */
  def composeK_[Message](
      f: Message => Message
  ): PolyFunction[Writer[Message, Message, *], Writer[Message, Message, *]] =
    composeK(f)

  /**
    * Lifts an piping transformation that connects the output channel of a writer
    * to the data channel of another writer.
    */
  def pipeDataK[Message, Out](
      other: Writer[Message, Message, Out]
  ): PolyFunction[Writer[Message, Out, *], Writer[Message, Message, *]] =
    new PolyFunction[Writer[Message, Out, *], Writer[Message, Message, *]] {
      def apply[A](writer: Writer[Message, Out, A]): Writer[Message, Message, A] =
        writer.pipeData(other)
    }

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
