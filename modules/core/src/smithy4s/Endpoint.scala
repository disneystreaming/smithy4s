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

/**
  * A representation of a smithy operation.
  *
  * @tparam Op: the GADT of all operations in a service
  * @tparam I: the input type of the operation (Unit if N/A)
  * @tparam E: the error ADT of the operation (Nothing if N/A)
  * @tparam O: the output of the operation (Unit if N/A)
  * @tparam SI: the Streamed input of the operaton (Nothing if N/A)
  * @tparam SO: the Streamed output of the operaton (Nothing if N/A)
  *
  * This type carries references to the Schemas of the various types involved,
  * allowing to compile corresponding codecs.
  *
  * Optionally, an endpoint can have an `Errorable` which allows for matching
  * throwables against the errors the operation knows about (which form an ADT
  * in the Scala representation)
  *
  * NB: SI an SO respectively are derived from the @streaming trait in smithy.
  * If this trait is present in one on one of the members of Input/Output, the
  * member is removed from the Scala representation, in order to avoid polluting
  * datatypes that typically fit in memory with concerns of streaming (which can
  * be encoded a great many ways, using a great many libraries)
  */
trait Endpoint[Op[_, _, _, _, _], I, E, O, SI, SO]
    extends Endpoint.Base[I, E, O, SI, SO] {

  def wrap(input: I): Op[I, E, O, SI, SO]

  object Error {
    def unapply(throwable: Throwable): Option[(Errorable[E], E)] =
      errorable.flatMap { err =>
        err.liftError(throwable).map(err -> _)
      }
  }
}

object Endpoint {

  trait Base[I, E, O, SI, SO] {
    def id: ShapeId
    final def name: String = id.name
    def hints: Hints
    def input: Schema[I]
    def output: Schema[O]
    def errorable: Option[Errorable[E]]
    def streamedInput: StreamingSchema[SI]
    def streamedOutput: StreamingSchema[SO]

  }

  type ForOperation[Op[_, _, _, _, _]] = {
    type e[I, E, O, SI, SO] = Endpoint[Op, I, E, O, SI, SO]
  }

  final case class Builder[I, E, O, SI, SO] private (
      private val baseId: ShapeId,
      private val baseHints: Hints,
      private val baseInput: Schema[I],
      private val baseOutput: Schema[O],
      private val baseErrorable: Option[Errorable[E]],
      private val baseStreamedInput: StreamingSchema[SI],
      private val baseStreamedOutput: StreamingSchema[SO]
  ) {

    def withId(id: ShapeId): Builder[I, E, O, SI, SO] = copy(baseId = id)

    def mapId(f: ShapeId => ShapeId): Builder[I, E, O, SI, SO] =
      copy(baseId = f(baseId))

    def withHints(hints: Hints): Builder[I, E, O, SI, SO] =
      copy(baseHints = hints)

    def mapHints(f: Hints => Hints): Builder[I, E, O, SI, SO] =
      copy(baseHints = f(baseHints))

    def withInput(input: Schema[I]): Builder[I, E, O, SI, SO] =
      copy(baseInput = input)

    def mapInput(f: Schema[I] => Schema[I]): Builder[I, E, O, SI, SO] =
      copy(baseInput = f(baseInput))

    def withOutput(output: Schema[O]): Builder[I, E, O, SI, SO] =
      copy(baseOutput = output)

    def mapOutput(f: Schema[O] => Schema[O]): Builder[I, E, O, SI, SO] =
      copy(baseOutput = f(baseOutput))

    def withErrorable(
        errorable: Option[Errorable[E]]
    ): Builder[I, E, O, SI, SO] = copy(baseErrorable = errorable)

    def mapErrorable(
        f: Option[Errorable[E]] => Option[Errorable[E]]
    ): Builder[I, E, O, SI, SO] = copy(baseErrorable = f(baseErrorable))

    def withStreamedInput(
        streamedInput: StreamingSchema[SI]
    ): Builder[I, E, O, SI, SO] = copy(baseStreamedInput = streamedInput)

    def mapStreamedInput(
        f: StreamingSchema[SI] => StreamingSchema[SI]
    ): Builder[I, E, O, SI, SO] = copy(baseStreamedInput = f(baseStreamedInput))

    def withSteamedOutput(
        streamedOutput: StreamingSchema[SO]
    ): Builder[I, E, O, SI, SO] = copy(baseStreamedOutput = streamedOutput)

    def mapSteamedOutput(
        f: StreamingSchema[SO] => StreamingSchema[SO]
    ): Builder[I, E, O, SI, SO] =
      copy(baseStreamedOutput = f(baseStreamedOutput))

    def build: Endpoint.Base[I, E, O, SI, SO] =
      new Endpoint.Base[I, E, O, SI, SO] {
        override val id: ShapeId = baseId

        override val hints: Hints = baseHints

        override val input: Schema[I] = baseInput

        override val output: Schema[O] = baseOutput

        override val errorable: Option[Errorable[E]] = baseErrorable

        override val streamedInput: StreamingSchema[SI] = baseStreamedInput

        override val streamedOutput: StreamingSchema[SO] = baseStreamedOutput
      }
  }

  object Builder {
    def fromEndpoint[I, E, O, SI, SO](
        endpoint: Endpoint.Base[I, E, O, SI, SO]
    ): Builder[I, E, O, SI, SO] = {
      Builder(
        baseId = endpoint.id,
        baseHints = endpoint.hints,
        baseInput = endpoint.input,
        baseOutput = endpoint.output,
        baseErrorable = endpoint.errorable,
        baseStreamedInput = endpoint.streamedInput,
        baseStreamedOutput = endpoint.streamedOutput
      )
    }
  }
}
