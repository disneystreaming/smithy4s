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

import cats.data.Kleisli
import cats.effect.Concurrent
import cats.syntax.all._
import org.http4s.HttpApp
import org.http4s.Method
import org.http4s.Entity
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.kinds._

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
      makeServerCodecs: HttpUnaryServerCodecs.Make[F, Entity[F]],
      middleware: ServerEndpointMiddleware.EndpointMiddleware[F, Op],
  ): Either[HttpEndpoint.HttpEndpointError,SmithyHttp4sServerEndpoint[F]] =
  // format: on
    HttpEndpoint.cast(endpoint).flatMap { httpEndpoint =>
      fromSmithy4sHttpMethod(httpEndpoint.method)
        .leftMap { e =>
          HttpEndpoint.HttpEndpointError(
            "Couldn't parse HTTP method: " + e
          )
        }
        .map { method =>
          new SmithyHttp4sServerEndpointImpl[F, Op, I, E, O, SI, SO](
            impl,
            endpoint,
            method,
            httpEndpoint,
            makeServerCodecs,
            middleware
          )
        }
    }

}

// format: off
private[http4s] class SmithyHttp4sServerEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
    impl: FunctorInterpreter[Op, F],
    endpoint: Endpoint[Op, I, E, O, SI, SO],
    val method: Method,
    httpEndpoint: HttpEndpoint[I],
    makeServerCodecs: UnaryServerCodecs.Make[F],
    middleware: ServerEndpointMiddleware.EndpointMiddleware[F, Op],
)(implicit F: Concurrent[F]) extends SmithyHttp4sServerEndpoint[F] {

  val serverCodecs = makeServerCodecs(endpoint)
  import serverCodecs._
  val contractErrorResponseEncoder: HttpResponse.Encoder[Entity[F], HttpContractError] =
    serverCodecs.errorEncoder(HttpContractError.schema)
  // format: on

  def matches(path: Array[String]): Option[PathParams] = {
    httpEndpoint.matches(path)
  }

  override val httpApp: HttpApp[F] = {
    val baseApp = HttpApp[F] { req =>
      val run: F[O] = for {
        input <- inputDecoder.read(toSmithy4sHttpRequest(req))
        output <- (impl(endpoint.wrap(input)): F[O])
      } yield output

      run
        .map(outputEncoder.write(successResponseBase, _))
        .map(fromSmithy4sHttpResponse)
    }
    middleware(endpoint)(baseApp).handleErrorWith(error =>
      Kleisli.liftF(errorResponse(error).map(fromSmithy4sHttpResponse))
    )
  }

  private val successResponseBase: HttpResponse[Entity[F]] =
    HttpResponse(httpEndpoint.code, Map.empty, Entity.empty)
  // Response[F](Status.fromInt(httpEndpoint.code).getOrElse(Status.Ok))
  private val internalErrorBase: HttpResponse[Entity[F]] =
    HttpResponse(500, Map.empty, Entity.empty)
  private val badRequestBase: HttpResponse[Entity[F]] =
    HttpResponse(400, Map.empty, Entity.empty)

  def errorResponse(throwable: Throwable): F[HttpResponse[Entity[F]]] =
    throwable match {
      case e: HttpContractError =>
        F.pure(contractErrorResponseEncoder.write(badRequestBase, e))
      case endpoint.Error((_, e)) =>
        F.pure(errorEncoder.write(internalErrorBase, e))
      case e: Throwable =>
        F.raiseError(e)
    }

}
