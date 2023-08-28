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
package internals

import cats.effect.Concurrent
import org.http4s.HttpApp
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.kinds._
import smithy4s.server.UnaryServerCodecs
import smithy4s.interopcats._

/**
  * A construct that encapsulates a smithy4s endpoint, and exposes
  * http4s specific semantics.
  */
// scalafmt { maxColumn = 120}
private[http4s] trait SmithyHttp4sServerEndpoint[F[_]] {
  def method: org.http4s.Method
  def matches(path: Array[String]): Option[PathParams]
  def httpApp: HttpApp[F]

  def matchTap(
      path: Array[String]
  ): Option[(SmithyHttp4sServerEndpoint[F], PathParams)] =
    matches(path).map(this -> _)
}

private[http4s] object SmithyHttp4sServerEndpoint {

  // format: off
  def make[F[_]: Concurrent, Op[_, _, _, _, _], I, E, O, SI, SO](
      impl: FunctorInterpreter[Op, F],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      makeServerCodecs: UnaryServerCodecs[F, Request[F], Response[F], I, E, O],
      middleware: HttpApp[F] => HttpApp[F]
  ): Either[HttpEndpoint.HttpEndpointError,SmithyHttp4sServerEndpoint[F]] =
  // format: on
    HttpEndpoint.cast(endpoint).flatMap { httpEndpoint =>
      val maybeHandler =
        fromSmithy4sHttpMethod(httpEndpoint.method).map { method =>
          new SmithyHttp4sServerEndpointImpl[F, Op, I, E, O, SI, SO](
            impl,
            endpoint,
            method,
            httpEndpoint,
            makeServerCodecs,
            middleware
          )
        }
      maybeHandler match {
        case Some(handler) => Right(handler)
        case None =>
          Left(HttpEndpoint.HttpEndpointError("Could not parse method"))
      }
    }

}

// format: off
private[http4s] class SmithyHttp4sServerEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
    impl: FunctorInterpreter[Op, F],
    endpoint: Endpoint[Op, I, E, O, SI, SO],
    val method: Method,
    httpEndpoint: HttpEndpoint[I],
    serverCodecs: UnaryServerCodecs[F, Request[F], Response[F], I, E, O],
    middleware: HttpApp[F] => HttpApp[F],
)(implicit F: Concurrent[F]) extends SmithyHttp4sServerEndpoint[F] {

  def matches(path: Array[String]): Option[PathParams] = {
    httpEndpoint.matches(path)
  }

  private val adaptedMiddleware
      : (Request[F] => F[Response[F]]) => (Request[F] => F[Response[F]]) =
    router => middleware(HttpApp(router)).run

  val httpApp = HttpApp(
    smithy4s.server.UnaryServerEndpoint(
      impl,
      endpoint,
      serverCodecs,
      adaptedMiddleware
    )
  )

}
