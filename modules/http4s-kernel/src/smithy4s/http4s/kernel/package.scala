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

package smithy4s.http4s

import cats.effect.SyncIO
import cats.syntax.all._
import org.http4s._
import org.typelevel.ci.CIString
import org.typelevel.vault.Key
import smithy4s.Blob
import smithy4s.http.CaseInsensitive
import smithy4s.http.PathParams
import smithy4s.http.{HttpUriScheme => Smithy4sHttpUriScheme}
import smithy4s.http.{HttpMethod => Smithy4sHttpMethod}
import smithy4s.http.{HttpRequest => Smithy4sHttpRequest}
import smithy4s.http.{HttpResponse => Smithy4sHttpResponse}
import smithy4s.http.{HttpUri => Smithy4sHttpUri}
import cats.MonadThrow
import cats.effect.Concurrent
import fs2.Stream
import fs2.Chunk

// scalafmt: { maxColumn = 120}
package object kernel {

  def toSmithy4sHttpRequest[F[_]: Concurrent](req: Request[F]): F[Smithy4sHttpRequest[Blob]] = {
    val pathParams = req.attributes.lookup(pathParamsKey)
    val uri = toSmithy4sHttpUri(req.uri, pathParams)
    val headers = getHeaders(req)
    val method = toSmithy4sHttpMethod(req.method)
    collectBytes(req.body).map { blob =>
      Smithy4sHttpRequest(method, uri, headers, blob)
    }
  }

  def fromSmithy4sHttpRequest[F[_]: MonadThrow](req: Smithy4sHttpRequest[Blob]): Request[F] = {
    val method = unsafeFromSmithy4sHttpMethod(req.method)
    val headers = toHeaders(req.headers)
    val updatedHeaders = req.body.size match {
      case 0             => headers
      case contentLength => headers.put("Content-Length" -> contentLength.toString)
    }
    Request(method, fromSmithy4sHttpUri(req.uri), headers = updatedHeaders, body = toStream(req.body))
  }

  def toSmithy4sHttpUri(uri: Uri, pathParams: Option[PathParams] = None): Smithy4sHttpUri = {
    val uriScheme = uri.scheme match {
      case Some(Uri.Scheme.https) => Smithy4sHttpUriScheme.Https
      case _                      => Smithy4sHttpUriScheme.Http
    }

    Smithy4sHttpUri(
      uriScheme,
      uri.host.map(_.renderString),
      uri.port,
      uri.path.segments.map(_.decoded()),
      getQueryParams(uri),
      pathParams
    )
  }

  def fromSmithy4sHttpResponse[F[_]](res: Smithy4sHttpResponse[Blob]): Response[F] = {
    val status = Status.fromInt(res.statusCode) match {
      case Right(value) => value
      case Left(e)      => throw e
    }

    val headers = toHeaders(res.headers)
    val updatedHeaders = {
      val contentLength = res.body.size
      if (contentLength <= 0) headers
      else headers.put("Content-Length" -> contentLength.toString)
    }
    Response(status, headers = updatedHeaders, body = toStream(res.body))
  }

  def toSmithy4sHttpResponse[F[_]: Concurrent](res: Response[F]): F[Smithy4sHttpResponse[Blob]] =
    collectBytes(res.body).map { blob =>
      val headers = res.headers.headers
        .map(h => CaseInsensitive(h.name.toString) -> Seq(h.value))
        .toMap
      Smithy4sHttpResponse(res.status.code, headers, blob)
    }

  def fromSmithy4sHttpUri(uri: Smithy4sHttpUri): Uri = {
    val path = Uri.Path.Root.addSegments(uri.path.map(Uri.Path.Segment(_)).toVector)
    val authority = uri.host.map(h => Uri.Authority(host = Uri.RegName(h), port = uri.port))
    Uri(
      path = path,
      authority = authority,
      scheme = Some {
        uri.scheme match {
          case Smithy4sHttpUriScheme.Http  => Uri.Scheme.http
          case Smithy4sHttpUriScheme.Https => Uri.Scheme.https
        }
      }
    ).withMultiValueQueryParams(uri.queryParams)
  }

  /**
    * A vault key that is used to store extracted path-parameters into request during
    * the routing logic.
    *
    * The http path matching logic extracts the relevant segment of the URI in order
    * to verify that a request corresponds to an endpoint. This information MUST be stored
    * in the request before any decoding of metadata is attempted, as it'll fail otherwise.
    */
  private[smithy4s] val pathParamsKey: Key[PathParams] =
    Key.newKey[SyncIO, PathParams].unsafeRunSync()

  private[smithy4s] def unsafeFromSmithy4sHttpMethod(
      method: Smithy4sHttpMethod
  ): Method =
    method match {
      case Smithy4sHttpMethod.PUT    => Method.PUT
      case Smithy4sHttpMethod.POST   => Method.POST
      case Smithy4sHttpMethod.DELETE => Method.DELETE
      case Smithy4sHttpMethod.GET    => Method.GET
      case Smithy4sHttpMethod.PATCH  => Method.PATCH
      case o: Smithy4sHttpMethod.OTHER =>
        Method.fromString(o.value) match {
          case Left(e)  => throw e
          case Right(m) => m
        }
    }

  private[smithy4s] def fromSmithy4sHttpMethod(
      method: Smithy4sHttpMethod
  ): Option[Method] =
    method match {
      case Smithy4sHttpMethod.PUT    => Some(Method.PUT)
      case Smithy4sHttpMethod.POST   => Some(Method.POST)
      case Smithy4sHttpMethod.DELETE => Some(Method.DELETE)
      case Smithy4sHttpMethod.GET    => Some(Method.GET)
      case Smithy4sHttpMethod.PATCH  => Some(Method.PATCH)
      case o: Smithy4sHttpMethod.OTHER =>
        Method.fromString(o.value) match {
          case Left(_)  => None
          case Right(m) => Some(m)
        }
    }

  private[smithy4s] def toSmithy4sHttpMethod(method: Method): Smithy4sHttpMethod =
    method match {
      case Method.PUT    => Smithy4sHttpMethod.PUT
      case Method.POST   => Smithy4sHttpMethod.POST
      case Method.DELETE => Smithy4sHttpMethod.DELETE
      case Method.GET    => Smithy4sHttpMethod.GET
      case Method.PATCH  => Smithy4sHttpMethod.PATCH
      case other         => Smithy4sHttpMethod.OTHER(other.renderString)
    }

  private[smithy4s] def toHeaders(mp: Map[CaseInsensitive, Seq[String]]) =
    Headers(mp.flatMap { case (k, v) =>
      v.map(Header.Raw(CIString(k.toString), _))
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
      uri: Uri
  ): Map[String, List[String]] =
    uri.query.pairs
      .collect {
        case (name, None)        => name -> "true"
        case (name, Some(value)) => name -> value
      }
      .groupBy(_._1)
      .map { case (k, v) => k -> v.map(_._2).toList }

  private def collectBytes[F[_]: Concurrent](
      stream: fs2.Stream[F, Byte]
  ): F[Blob] = stream.chunks.compile
    .to(Chunk)
    .map(_.flatten)
    .map(chunk => Blob(chunk.toArray))

  private def toStream[F[_]](
      blob: Blob
  ): Stream[F, Byte] = Stream.chunk(Chunk.array(blob.toArray))

}
