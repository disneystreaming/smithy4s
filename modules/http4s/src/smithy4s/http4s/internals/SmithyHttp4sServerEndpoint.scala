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
import cats.syntax.all._
import org.http4s.Method
import org.http4s.Response
import org.http4s.Status
import smithy4s.http._
import smithy4s.kinds._
import org.http4s.HttpApp
import smithy4s.http4s.kernel._
import cats.effect.Concurrent

/**
  * A construct that encapsulates a smithy4s endpoint, and exposes
  * http4s specific semantics.
  */
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
      makeServerCodecs: UnaryServerCodecs.Make[F],
      errorTransformation: PartialFunction[Throwable, F[Throwable]],
      middleware: ServerEndpointMiddleware.EndpointMiddleware[F, Op],
  ): Either[HttpEndpoint.HttpEndpointError,SmithyHttp4sServerEndpoint[F]] =
  // format: on
    HttpEndpoint.cast(endpoint).flatMap { httpEndpoint =>
      toHttp4sMethod(httpEndpoint.method)
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
            errorTransformation,
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
    errorTransformation: PartialFunction[Throwable, F[Throwable]],
    middleware: ServerEndpointMiddleware.EndpointMiddleware[F, Op],
)(implicit F: Concurrent[F]) extends SmithyHttp4sServerEndpoint[F] {

  val serverCodecs = makeServerCodecs(endpoint)
  import serverCodecs._
  val contractErrorResponseEncoder: ResponseEncoder[F, HttpContractError] = serverCodecs.errorEncoder(HttpContractError.schema)
  // format: on

  def matches(path: Array[String]): Option[PathParams] = {
    httpEndpoint.matches(path)
  }

  private val applyMiddleware = middleware(endpoint)

  override val httpApp: HttpApp[F] =
    applyMiddleware(HttpApp[F] { req =>
      val run: F[O] = for {
        input <- inputDecoder.decodeRequest(req)
        output <- (impl(endpoint.wrap(input)): F[O])
      } yield output

      run
        .recoverWith(transformError)
        .map(outputEncoder.addToResponse(successResponseBase, _))
    }).handleErrorWith(error => Kleisli.liftF(errorResponse(error)))

  private val transformError: PartialFunction[Throwable, F[O]] = {
    case e @ endpoint.Error(_, _) => F.raiseError(e)
    case scala.util.control.NonFatal(other)
        if errorTransformation.isDefinedAt(other) =>
      errorTransformation(other).flatMap(F.raiseError)
  }

  private val successResponseBase: Response[F] =
    Response[F](Status.fromInt(httpEndpoint.code).getOrElse(Status.Ok))
  private val internalErrorBase: Response[F] =
    Response[F](Status.InternalServerError)
  private val badRequestBase: Response[F] = Response[F](Status.BadRequest)

  def errorResponse(throwable: Throwable): F[Response[F]] = throwable match {
    case e: HttpContractError =>
      F.pure(contractErrorResponseEncoder.addToResponse(badRequestBase, e))
    case endpoint.Error((_, e)) =>
      F.pure(errorEncoder.addToResponse(internalErrorBase, e))
    case e: Throwable =>
      F.raiseError(e)
  }

}
