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

import cats.MonadThrow
import cats.data.Kleisli
import org.http4s.Response
import org.http4s.HttpApp
import cats.implicits._

object ServerEndpointMiddleware {

  trait Simple[F[_]] extends Endpoint.Middleware.Simple[HttpApp[F]]

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

}
