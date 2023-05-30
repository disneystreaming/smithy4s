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

package smithy4s.http4s

import org.http4s.Header
import org.http4s.Headers
import org.http4s.Media
import org.http4s.{Method => Http4sMethod}
import org.typelevel.ci.CIString
import smithy4s.http.CaseInsensitive
import smithy4s.http.PathParams
import smithy4s.http.{HttpMethod => SmithyMethod}
import org.http4s.ParseFailure
import cats.syntax.all._
import smithy4s.http.Metadata
import org.http4s.Request
import org.http4s.Response
import smithy4s.capability.Covariant
import smithy4s.ConstraintError
import org.http4s.EntityDecoder
import cats.Monad
import org.http4s.DecodeResult
import org.http4s.InvalidMessageBodyFailure
import org.typelevel.vault.Key
import cats.effect.SyncIO

package object kernel {

  /**
    * A vault key that is used to store extracted path-parameters into request during
    * the routing logic.
    *
    * The http path matching logic extracts the relevant segment of the URI in order
    * to verify that a request corresponds to an endpoint. This information MUST be stored
    * in the request before any decoding of metadata is attempted, as it'll fail otherwise.
    */
  val pathParamsKey: Key[PathParams] =
    Key.newKey[SyncIO, PathParams].unsafeRunSync()

  private[smithy4s] def toHttp4sMethod(
      method: SmithyMethod
  ): Either[ParseFailure, Http4sMethod] =
    method match {
      case smithy4s.http.HttpMethod.PUT      => Http4sMethod.PUT.asRight
      case smithy4s.http.HttpMethod.POST     => Http4sMethod.POST.asRight
      case smithy4s.http.HttpMethod.DELETE   => Http4sMethod.DELETE.asRight
      case smithy4s.http.HttpMethod.GET      => Http4sMethod.GET.asRight
      case smithy4s.http.HttpMethod.PATCH    => Http4sMethod.PATCH.asRight
      case smithy4s.http.HttpMethod.OTHER(v) => Http4sMethod.fromString(v)
    }

  private[smithy4s] def toHeaders(mp: Map[CaseInsensitive, Seq[String]]) =
    Headers(mp.flatMap { case (k, v) =>
      v.filterNot(_.isEmpty).map(Header.Raw(CIString(k.toString), _))
    }.toList)

  private[smithy4s] def getFirstHeader[F[_]](
      req: Media[F],
      s: String
  ): Option[String] =
    req.headers.get(CIString(s)).map(_.head.value)

  private[smithy4s] def toMap(hd: Headers) = hd.headers.map { h =>
    h.name.toString -> h.value
  }.toMap

  private[smithy4s] def getHeaders[F[_]](req: Media[F]) =
    req.headers.headers.groupBy(_.name).map { case (k, v) =>
      (CaseInsensitive(k.toString), v.map(_.value))
    }

  private[smithy4s] def getQueryParams[F[_]](
      request: Request[F]
  ): Map[String, List[String]] =
    request.uri.query.pairs
      .collect {
        case (name, None)        => name -> "true"
        case (name, Some(value)) => name -> value
      }
      .groupBy(_._1)
      .map { case (k, v) => k -> v.map(_._2).toList }

  private[smithy4s] def getRequestMetadata[F[_]](
      pathParams: PathParams,
      request: Request[F]
  ): Metadata =
    Metadata(
      path = pathParams,
      query = getQueryParams(request),
      headers = getHeaders(request),
      statusCode = None
    )

  private[smithy4s] def getResponseMetadata[F[_]](
      response: Response[F]
  ): Metadata =
    Metadata(
      headers = getHeaders(response),
      statusCode = Some(response.status.code)
    )

  implicit def covariantEntityDecoder[F[_]](implicit
      F: Monad[F]
  ): Covariant[EntityDecoder[F, *]] = new Covariant[EntityDecoder[F, *]] {
    def map[A, B](fa: EntityDecoder[F, A])(f: A => B): EntityDecoder[F, B] =
      fa.map(f)

    def emap[A, B](
        fa: EntityDecoder[F, A]
    )(f: A => Either[ConstraintError, B]): EntityDecoder[F, B] =
      fa.flatMapR(a =>
        f(a) match {
          case Left(ce) =>
            DecodeResult.failureT(InvalidMessageBodyFailure(ce.message))
          case Right(value) => DecodeResult.successT(value)
        }
      )
  }
}
