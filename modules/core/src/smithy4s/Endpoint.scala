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
// scalafmt: {maxColumn = 120}
trait Endpoint[Op[_, _, _, _, _], I, E, O, SI, SO] extends Endpoint.Base[I, E, O, SI, SO] {

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

  trait Middleware[A] { self =>
    def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(endpoint: service.Endpoint[_, _, _, _, _]): A => A

    final def biject[B](to: A => B)(from: B => A): Middleware[B] = new Middleware[B] {
      def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(endpoint: service.Endpoint[_, _, _, _, _]): B => B =
        self.prepare(service)(endpoint).compose(from).andThen(to)
    }

    final def andThen(other: Middleware[A]): Middleware[A] =
      new Middleware[A] {
        def prepare[Alg[_[_, _, _, _, _]]](
            service: Service[Alg]
        )(endpoint: service.Endpoint[_, _, _, _, _]): A => A =
          self
            .prepare(service)(endpoint)
            .andThen(other.prepare(service)(endpoint))
      }

  }
// format: on

  object Middleware {

    trait Simple[Construct] extends Middleware[Construct] {
      def prepareWithHints(serviceHints: Hints, endpointHints: Hints): Construct => Construct

      final def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: service.Endpoint[_, _, _, _, _]
      ): Construct => Construct =
        prepareWithHints(service.hints, endpoint.hints)
    }

    def noop[Construct]: Middleware[Construct] =
      new Middleware[Construct] {
        override def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
            endpoint: service.Endpoint[_, _, _, _, _]
        ): Construct => Construct = identity
      }

  }

}
