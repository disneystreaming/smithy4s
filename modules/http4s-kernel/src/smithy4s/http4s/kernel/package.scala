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

import cats.Applicative
import cats.effect.SyncIO
import cats.syntax.all._
import org.http4s.Entity
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Media
import org.http4s.ParseFailure
import org.http4s.Request
import org.http4s.Response
import org.http4s.{Method => Http4sMethod}
import org.http4s.Status
import org.http4s.Uri
import org.typelevel.ci.CIString
import org.typelevel.vault.Key
import smithy4s.kinds.PolyFunctions
import smithy4s.Blob
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs._
import smithy4s.http.HttpContractError
import smithy4s.http.CaseInsensitive
import smithy4s.http.PathParams
import smithy4s.http.{HttpUriScheme => Smithy4sHttpUriScheme}
import smithy4s.http.{HttpMethod => SmithyMethod}
import smithy4s.http.{HttpRequest => Smithy4sHttpRequest}
import smithy4s.http.{HttpResponse => Smithy4sHttpResponse}
import smithy4s.http.{HttpUri => Smithy4sHttpUri}
import smithy4s.kinds.PolyFunction
import cats.MonadThrow
import cats.effect.Concurrent
import fs2.Stream
import fs2.Chunk

// scalafmt: { maxColumn = 120}
package object kernel {

  type UnaryServerCodecs[F[_], I, E, O] =
    smithy4s.http.HttpUnaryServerCodecs[F, Entity[F], I, E, O]
  object UnaryServerCodecs {
    type Make[F[_]] =
      smithy4s.http.HttpUnaryServerCodecs.Make[F, Entity[F]]
  }

  type EntityWriter[F[_], A] = Writer[Any, Entity[F], A]
  object EntityWriter {
    def fromPayloadWriterK[F[_]]: PolyFunction[PayloadWriter, EntityWriter[F, *]] =
      Writer
        .addingTo[Any]
        .andThenK((blob: Blob) =>
          Entity(
            Stream.chunk(Chunk.array(blob.toArray)),
            Some(blob.size.toLong)
          )
        )
  }

  type EntityReader[F[_], A] = Reader[F, Entity[F], A]

  object EntityReader {

    def fromPayloadReaderK[F[_]: Concurrent]: PolyFunction[PayloadReader, EntityReader[F, *]] =
      Reader
        .of[Blob]
        .liftPolyFunction(
          PolyFunctions
            .mapErrorK(HttpContractError.fromPayloadError)
            .andThen(MonadThrowLike.liftEitherK[F, HttpContractError])
        )
        .andThen(Reader.in[F].flatComposeK(collectBytes[F]))

    private def collectBytes[F[_]: Concurrent](
        entity: Entity[F]
    ): F[Blob] = entity.body.chunks.compile
      .to(Chunk)
      .map(_.flatten)
      .map(chunk => Blob(chunk.toArray))

  }

  private[smithy4s] def toSmithy4sHttpRequest[F[_]](
      req: Request[F]
  ): Smithy4sHttpRequest[Entity[F]] = {
    val pathParams = req.attributes.lookup(pathParamsKey)
    val uri = toSmithy4sHttpUri(req.uri, pathParams)
    val headers = getHeaders(req)
    val method = toSmithy4sMethod(req.method)
    Smithy4sHttpRequest(method, uri, headers, Entity(req.body, req.contentLength))
  }

  private[smithy4s] def fromSmithy4sHttpRequest[F[_]: MonadThrow](req: Smithy4sHttpRequest[Entity[F]]): F[Request[F]] =
    for {
      method <- fromSmithy4sHttpMethod(req.method).liftTo[F]
    } yield {
      Request(method, fromSmithy4sHttpUri(req.uri), headers = toHeaders(req.headers), body = req.body.body)
    }

  private[smithy4s] def toSmithy4sHttpUri(uri: Uri, pathParams: Option[PathParams] = None): Smithy4sHttpUri = {
    val uriScheme = uri.scheme match {
      case Some(Uri.Scheme.http) => Smithy4sHttpUriScheme.Http
      case _                     => Smithy4sHttpUriScheme.Https
    }
    Smithy4sHttpUri(
      uriScheme,
      uri.host.map(_.renderString).getOrElse("localhost"),
      uri.port,
      uri.path.segments.map(_.encoded),
      getQueryParams(uri),
      pathParams
    )
  }

  private[smithy4s] def fromSmithy4sHttpUri(uri: Smithy4sHttpUri): Uri = {
    val path = Uri.Path.Root.addSegments(uri.path.map(Uri.Path.Segment(_)).toVector)
    Uri(
      path = path,
      authority = Some(Uri.Authority(host = Uri.RegName(uri.host), port = uri.port))
    ).withMultiValueQueryParams(uri.queryParams)
  }

  private[smithy4s] def fromSmithy4sHttpResponse[F[_]](
      res: Smithy4sHttpResponse[Entity[F]]
  ): Response[F] = {
    val status =
      Status
        .fromInt(res.statusCode)
        .getOrElse(
          throw new IllegalStateException(
            s"Invalid status code ${res.statusCode}"
          )
        )
    val contentLength: Option[Header.ToRaw] =
      res.body.length.map(l => org.http4s.headers.`Content-Length`(l))

    val rawHeaders: Seq[Header.ToRaw] =
      res.headers.toSeq.map { case (name, values) =>
        Header.ToRaw.rawToRaw(
          Header.Raw(CIString(name.value), values.mkString(","))
        )
      }
    val headers = Headers(rawHeaders ++ contentLength)
    Response(status, headers = headers, body = res.body.body)
  }

  private[smithy4s] def toSmithy4sHttpResponse[F[_]](
      res: Response[F]
  ): Smithy4sHttpResponse[Entity[F]] = Smithy4sHttpResponse[Entity[F]](
    res.status.code,
    res.headers.headers
      .map(h => CaseInsensitive(h.name.toString) -> Seq(h.value))
      .toMap,
    Entity(
      res.body,
      res.headers.get[org.http4s.headers.`Content-Length`].map(_.length)
    )
  )

  type UnaryClientCodecs[F[_], I, E, O] =
    smithy4s.http.HttpUnaryClientCodecs[F, Entity[F], I, E, O]
  object UnaryClientCodecs {
    type Make[F[_]] =
      smithy4s.http.HttpUnaryClientCodecs.Make[F, Entity[F]]
  }

  type ResponseEncoder[F[_], A] =
    smithy4s.codecs.Writer[Response[F], Response[F], A]
  type RequestEncoder[F[_], A] =
    smithy4s.codecs.Writer[Request[F], Request[F], A]
  type MediaDecoder[F[_], A] = smithy4s.codecs.Reader[F, Media[F], A]
  type RequestDecoder[F[_], A] = smithy4s.codecs.Reader[F, Request[F], A]
  type ResponseDecoder[F[_], A] = smithy4s.codecs.Reader[F, Response[F], A]

  private[smithy4s] implicit def monadThrowShim[F[_]: MonadThrow]: MonadThrowLike[F] =
    new MonadThrowLike[F] {
      def pure[A](a: A): F[A] = Applicative[F].pure(a)
      def zipMapAll[A](seq: IndexedSeq[F[Any]])(f: IndexedSeq[Any] => A): F[A] =
        seq.toVector.asInstanceOf[Vector[F[Any]]].sequence.map(f)
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        MonadThrow[F].flatMap(fa)(f)
      def raiseError[A](e: Throwable): F[A] = MonadThrow[F].raiseError(e)
    }

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

  private[smithy4s] def fromSmithy4sHttpMethod(
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

  private[smithy4s] def toSmithy4sMethod(method: Http4sMethod): SmithyMethod =
    method match {
      case Http4sMethod.PUT    => SmithyMethod.PUT
      case Http4sMethod.POST   => SmithyMethod.POST
      case Http4sMethod.DELETE => SmithyMethod.DELETE
      case Http4sMethod.GET    => SmithyMethod.GET
      case Http4sMethod.PATCH  => SmithyMethod.PATCH
      case other               => SmithyMethod.OTHER(other.renderString)
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

  private[smithy4s] def toStrict[F[_]: Concurrent](entity: Entity[F]): F[(Entity[F], Blob)] = {
    entity.body.chunks.compile
      .to(Chunk)
      .map(_.flatten)
      .map(chunk => Entity(Stream.chunk(chunk), Some(chunk.size.toLong)) -> Blob(chunk.toArray))
  }

}
