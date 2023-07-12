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
package http4s

import cats.Monoid
import cats.MonadThrow
import cats.data.Kleisli
import org.http4s.Response
import cats.implicits._
import org.http4s.HttpApp

// format: off
trait ServerEndpointMiddleware[F[_]] {
  self  => 
  def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
      endpoint: Endpoint[service.Operation, _, _, _, _, _]
  ): HttpApp[F] => HttpApp[F]

  def andThen(other: ServerEndpointMiddleware[F]): ServerEndpointMiddleware[F] = 
    new ServerEndpointMiddleware[F] {
      def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): HttpApp[F] => HttpApp[F] =
        self.prepare(service)(endpoint).andThen(other.prepare(service)(endpoint))
    }
}
// format: on

object ServerEndpointMiddleware {

  trait Simple[F[_]] extends ServerEndpointMiddleware[F] {
    def prepareWithHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): HttpApp[F] => HttpApp[F]

    final def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: Endpoint[service.Operation, _, _, _, _, _]
    ): HttpApp[F] => HttpApp[F] =
      prepareWithHints(service.hints, endpoint.hints)
  }

  def mapErrors[F[_]: MonadThrow](
      f: PartialFunction[Throwable, Throwable]
  ): ServerEndpointMiddleware[F] =
    flatMapErrors(f.andThen(_.pure[F]))

  def flatMapErrors[F[_]: MonadThrow](
      f: PartialFunction[Throwable, F[Throwable]]
  ): ServerEndpointMiddleware[F] =
    new ServerEndpointMiddleware[F] {
      def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): HttpApp[F] => HttpApp[F] = http => {
        val handler: PartialFunction[Throwable, F[Throwable]] = {
          case e @ endpoint.Error(_, _) => e.raiseError[F, Throwable]
          case scala.util.control.NonFatal(other) if f.isDefinedAt(other) =>
            f(other).flatMap(_.raiseError[F, Throwable])

        }
        Kleisli(req =>
          http(req).recoverWith(
            handler.andThen(_.flatMap(_.raiseError[F, Response[F]]))
          )
        )
      }
    }

  private[http4s] type EndpointMiddleware[F[_], Op[_, _, _, _, _]] =
    Endpoint[Op, _, _, _, _, _] => HttpApp[F] => HttpApp[F]

  def noop[F[_]]: ServerEndpointMiddleware[F] =
    new ServerEndpointMiddleware[F] {
      override def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): HttpApp[F] => HttpApp[F] = identity
    }

  implicit def monoidServerEndpointMiddleware[F[_]]
      : Monoid[ServerEndpointMiddleware[F]] =
    new Monoid[ServerEndpointMiddleware[F]] {
      def combine(
          a: ServerEndpointMiddleware[F],
          b: ServerEndpointMiddleware[F]
      ): ServerEndpointMiddleware[F] =
        a.andThen(b)

      val empty: ServerEndpointMiddleware[F] =
        new ServerEndpointMiddleware[F] {
          def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
              endpoint: Endpoint[service.Operation, _, _, _, _, _]
          ): HttpApp[F] => HttpApp[F] =
            identity
        }
    }

}
