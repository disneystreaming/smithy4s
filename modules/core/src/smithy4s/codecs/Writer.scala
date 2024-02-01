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

import smithy4s.capability.EncoderK
import smithy4s.schema._

/**
  * An abstraction that codifies the notion of modifying a message with some additional information.
  *
  * This has two input channels:
  *   * one for the message that is being modified (Message)
  *   * one for the actual data (A)
  *
  * This is particularly useful for http requests/responses, where different subsets of data
  * have a different impact on different locations of the http message : some fields may
  * impact headers, some fields may impact the http body, other things that are driven from
  * static information (smithy traits) may lead to a transformation of the message.
  *
  * Having the ability to decompose the notion of encoding a piece of data into different
  * writers that can be composed together is powerful and helps centralising some complexity
  * in third-party agnostic code.
  *
  * @tparam Message: some data being modified with the added information contained by A.
  * @tparam A: the type of data that is being written into the output channel
  */
// scalafmt: {maxColumn = 120}
trait Writer[Message, -A] { self =>

  /**
    * Symbolises the action of writing some content `A` into an output `Out`, provided some context `In`
    */
  def write(message: Message, a: A): Message

  def combine[A0 <: A](other: Writer[Message, A0]): Writer[Message, A0] = new Writer[Message, A0] {
    def write(message: Message, a: A0): Message = other.write(self.write(message, a), a)
  }

  def compose(f: Message => Message): Writer[Message, A] = (m, a) => self.write(f(m), a)

  def andThen(f: Message => Message): Writer[Message, A] = (m, a) => f(self.write(m, a))

  /**
    * Contramap the data which this writer works. The writer is a contravariant-functor on `A`.
    */
  final def contramap[B](f: B => A): Writer[Message, B] =
    new Writer[Message, B] {
      def write(message: Message, b: B): Message =
        self.write(message, f(b))
    }

  /**
   * Transforms a writer into an Encoder by supplying an initial value.
   */
  final def toEncoder(initial: Message): Encoder[Message, A] = new Encoder[Message, A] {
    def encode(a: A): Message = self.write(initial, a)
  }

}

object Writer {

  def combineCompilers[Message](
      left: CachedSchemaCompiler[Writer[Message, *]],
      right: CachedSchemaCompiler[Writer[Message, *]]
  ): CachedSchemaCompiler[Writer[Message, *]] = new CachedSchemaCompiler[Writer[Message, *]] {

    type Cache = (left.Cache, right.Cache)
    def createCache(): Cache = (left.createCache(), right.createCache())
    def fromSchema[A](schema: Schema[A]): Writer[Message, A] =
      fromSchema(schema, createCache())
    def fromSchema[A](schema: Schema[A], cache: Cache): Writer[Message, A] = {
      val first: Writer[Message, A] = left.fromSchema(schema, cache._1)
      val second: Writer[Message, A] = right.fromSchema(schema, cache._2)
      first.combine(second)
    }

  }

  type CachedCompiler[Message] = CachedSchemaCompiler[Writer[Message, *]]

  /**
    * Creates an writer from a function.
    */
  def lift[Message, A](f: (Message, A) => Message): Writer[Message, A] = f(_, _)

  /**
    * Creates a writer which returns a constant value
    */
  def constant[Message](m: Message): Writer[Message, Any] = (_, _) => m

  /**
    * Creates a writer that returns its input as its output, without taking
    * the data into consideration
    */
  def noop[Message]: Writer[Message, Any] = (message, _) => message

  // format: off
  implicit def writerEncoderK[Message]: EncoderK[Writer[Message, *], Message => Message] =
    new EncoderK[Writer[Message, *], Message => Message] {
      def apply[A](fa: Writer[Message, A], a: A): Message => Message = fa.write(_, a)
      def absorb[A](f: A => (Message => Message)): Writer[Message, A] = new Writer[Message, A]{
        def write(message: Message, a: A): Message = f(a)(message)
      }
    }
  // format: on

}
