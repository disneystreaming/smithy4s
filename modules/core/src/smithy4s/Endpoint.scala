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

  case class Builders[I, E, O, SI, SO](
    shapeId: ShapeId,
    hintsX: Hints,
    inputX: Schema[I],
    outputX: Schema[O],
    errorableX: Option[Errorable[E]],
    streamedInputX: StreamingSchema[SI],
    streamedOutputX: StreamingSchema[SO]) {

    def withId(id: ShapeId): Builders[I, E, O, SI, SO] = copy(shapeId = id)
    def mapId(f: ShapeId => ShapeId): Builders[I, E, O, SI, SO] = copy(shapeId = f(shapeId))

    def withHints(hints: Hints): Builders[I, E, O, SI, SO] = copy(hintsX = hints)
    def mapHints(f: Hints => Hints): Builders[I, E, O, SI, SO] = copy(hintsX = f(hintsX))
    def withInput(input: Schema[I]):  Builders[I, E, O, SI, SO] = copy(inputX = input)
    def mapInput(f: Schema[I] => Schema[I]):  Builders[I, E, O, SI, SO] = copy(inputX = f(inputX))


    def withOutput(output: Schema[O]): Builders[I, E, O, SI, SO] = copy(outputX = output)
    def mapOutput(f: Schema[O] => Schema[O]): Builders[I, E, O, SI, SO] = copy(outputX = f(outputX))

    def withErrorable(errorable: Option[Errorable[E]]): Builders[I, E, O, SI, SO] = copy(errorableX = errorable)

    def mapErrorable(f: Option[Errorable[E]] => Option[Errorable[E]]): Builders[I, E, O, SI, SO] = copy(errorableX = f(errorableX))

    def withStreamedInput(streamedInput: StreamingSchema[SI]): Builders[I, E, O, SI, SO] = copy(streamedInputX = streamedInput)

    def mapStreamedInput(f: StreamingSchema[SI] => StreamingSchema[SI]): Builders[I, E, O, SI, SO] = copy(streamedInputX = f(streamedInputX))

    def withSteamedOutput(streamedOutput: StreamingSchema[SO]): Builders[I, E, O, SI, SO] = copy(streamedOutputX = streamedOutput)

    def mapSteamedOutput(f: StreamingSchema[SO] => StreamingSchema[SO]): Builders[I, E, O, SI, SO] = copy(streamedOutputX = f(streamedOutputX))

    def build(): Endpoint.Base[I, E, O, SI, SO]  = new Endpoint.Base[I, E, O, SI, SO]{
      override def id: ShapeId = shapeId

      override def hints: Hints = hintsX

      override def input: Schema[I] = inputX

      override def output: Schema[O] = outputX

      override def errorable: Option[Errorable[E]] = errorableX

      override def streamedInput: StreamingSchema[SI] = streamedInputX

      override def streamedOutput: StreamingSchema[SO] = streamedOutputX
    }
    def mapBuilder(f: Builders[I, E, O, SI, SO] => Endpoint.Base[I, E, O, SI, SO] ): Endpoint.Base[I, E, O, SI, SO] = f(this)
    def toEndpoint(endpoint: Endpoint.Base[I, E, O, SI, SO] ): Builders[I, E, O, SI, SO] = {
      Builders(
        shapeId = endpoint.id,
        hintsX = endpoint.hints,
        inputX = endpoint.input,
        outputX = endpoint.output,
        errorableX = endpoint.errorable,
        streamedInputX = endpoint.streamedInput,
        streamedOutputX = endpoint.streamedInput,
      )
    }
  }
}
