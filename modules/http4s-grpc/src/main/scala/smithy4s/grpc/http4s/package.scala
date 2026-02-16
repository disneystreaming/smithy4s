package smithy4s.grpc

import cats.effect.Concurrent
import cats.implicits._
import org.http4s.{ Headers, Media, Request, Response, Status => HttpStatus, Uri}
import smithy4s.Blob
import smithy4s.http.CaseInsensitive
import smithy4s.http.{HttpUri => Smithy4sHttpUri}
import smithy4s.http.{HttpUriScheme => Smithy4sHttpUriScheme}
import cats.MonadThrow
import org.http4s.Header
import org.typelevel.ci.CIString
import org.http4s.Method
import org.http4s.HttpVersion
import alloy.proto.StatusDetails
import smithy4s.schema.CachedSchemaCompiler
import java.util.Base64
import smithy4s.codecs.PayloadDecoder
import scala.util.Try
import smithy4s.codecs.PayloadEncoder

package object http4s {

  type PathParams = Map[String, String]

  def fromGrpcRequest[F[_]](
      req: GrpcRequest[Blob],
      encodePathSegments: Boolean
  ): Request[F] = {
    val headers = Headers(
      "Content-Type" -> "application/grpc+proto",
      "TE" -> "trailers",
    ) ++ toHeaders(req.metadata)

    Request[F](
      Method.POST,
      fromSmithy4sHttpUri(req.uri, encodePathSegments = encodePathSegments),
      headers = headers,
      body = toStream(req.body),
      httpVersion = HttpVersion.`HTTP/2`
    )
  }

  def toGrpcRequest[F[_]: Concurrent](req: Request[F]): F[GrpcRequest[Blob]] = {
      // val pathParams = req.attributes.lookup(pathParamsKey)
      val pathParams = Option.empty //FIXME: is this required?
      val uri = toSmithy4sHttpUri(req.uri, pathParams)
      val headers = getHeaders(req)

      //FIXME: figure out if we need the method in the request
      // we probably need it to reject requests other than POST?

      // val method = toSmithy4sHttpMethod(req.method)
      
      collectBytes(req.body).map { blob =>
        GrpcRequest(uri, headers, blob)
      }
    }

  // FIXME: Figure out a way to get rid of the compiler argument.
  // Ideally we shouldn't do any encoding here(?) and the GrpcResponse should have
  // all the data in the correct format/payload?
  def fromGrpcResponse[F[_]](res: GrpcResponse[Blob], compiler: CachedSchemaCompiler[PayloadEncoder[*]])(implicit F: MonadThrow[F]): Response[F] = {
    val encoder = compiler.fromSchema(StatusDetails.schema, compiler.createCache())

    val headers = Headers(
      "Content-Type" -> "application/grpc+proto",
      "TE" -> "trailers"
    )

    val status = res.status

    val trailers = F.pure{
      toHeaders(res.metadata)
        .put(GrpcHeaders.Status.name ->  status.code.value.toString())
        .put(GrpcHeaders.Message.name -> status.message.getOrElse(""))
        .put(GrpcHeaders.StatusDetailsBin.name -> encoder.encode(status.details).toBase64String)
    }

    Response(HttpStatus.Ok, headers = headers, body = toStream(res.body), httpVersion = HttpVersion.`HTTP/2`)
      .withTrailerHeaders(trailers)
  }

  // FIXME: Similar as `fromGrpcResponse` figure out a way to get rid of the compiler argument.
  def toGrpcResponse[F[_]](res: Response[F], compiler: CachedSchemaCompiler[PayloadDecoder[*]])(implicit F: Concurrent[F]): F[GrpcResponse[Blob]] = {
    val decoder = compiler.fromSchema(StatusDetails.schema, compiler.createCache())
    def extractStatus(headers: Headers): F[Status] = {
      val defaultSuccess = Status(StatusCode.Ok, Option.empty, StatusDetails(List.empty))

      GrpcHeaders.getSingle(headers, GrpcHeaders.Status).map { statusHeaderValue =>
          GrpcHeaders.Status.parse(statusHeaderValue).getOrElse(StatusCode.Unknown) match {
            case StatusCode.Ok => F.pure(defaultSuccess)
            case statusCode =>
              GrpcHeaders.getSingle(headers, GrpcHeaders.StatusDetailsBin).fold(F.pure(StatusDetails(List.empty))){detailsString =>
                F.fromTry(Try(Base64.getDecoder().decode(detailsString)))  //FIXME: handle the case when the value of the header is not base64 encoded
                  .flatMap(bytes => F.fromEither(decoder.decode(Blob(bytes)))) //FIXME: handle the case when error payload can't be decoded
              }
              .map(details => Status(statusCode, GrpcHeaders.getSingle(headers, GrpcHeaders.Message), details))
          }
      }.getOrElse(F.pure(defaultSuccess))
    }

    for {
      blob <- collectBytes(res.body)
      httpStatus <- F.pure(res.status)
      grpcStatus <- httpStatus match {
        case s if s.isSuccess =>
          res.trailerHeaders.flatMap(extractStatus)
        case s =>
          F.raiseError[Status](new RuntimeException(s"HTTP/2 transport failed with ${s}"))
      }
      headers = res.headers.headers
        .map(h => CaseInsensitive(h.name.toString) -> Seq(h.value))
        .toMap
    } yield GrpcResponse(grpcStatus, headers, blob)
  }

  private def toSmithy4sHttpUri(uri: Uri, pathParams: Option[PathParams]): Smithy4sHttpUri = {
    val uriScheme = uri.scheme match {
      case Some(Uri.Scheme.https) => Smithy4sHttpUriScheme.Https
      case _                      => Smithy4sHttpUriScheme.Http
    }

    Smithy4sHttpUri(
      uriScheme,
      uri.host.map(_.renderString).getOrElse("localhost"),
      uri.port,
      uri.path.segments.map(_.decoded()),
      Map.empty,
      // FIXME: what about query params? We don't really care about them, do we?
      // getQueryParams(uri),
      pathParams
    )
  }

  //FIXME: Methods below are lifted from http4s-kernel, figure out how to deduplicated
  private[smithy4s] def toHeaders(mp: Map[CaseInsensitive, Seq[String]]) =
    Headers(mp.flatMap { case (k, v) =>
      v.map(Header.Raw(CIString(k.toString), _))
    }.toList)


  private def fromSmithy4sHttpUri(uri: Smithy4sHttpUri, encodePathSegments: Boolean): Uri = {
    val mkSegment: String => Uri.Path.Segment =
      // Segment.apply will call pathEncode on the segment,
      // which is what we want if encodePathSegments is true.
      if (encodePathSegments) Uri.Path.Segment.apply
      else Uri.Path.Segment.encoded

    val path = Uri.Path.Root.addSegments(uri.path.map(mkSegment))

    Uri(
      path = path,
      authority = Some(Uri.Authority(host = Uri.RegName(uri.host), port = uri.port)),
      scheme = Some {
        uri.scheme match {
          case Smithy4sHttpUriScheme.Http  => Uri.Scheme.http
          case Smithy4sHttpUriScheme.Https => Uri.Scheme.https
        }
      }
    ).withMultiValueQueryParams(uri.queryParams)
  }

  private def collectBytes[F[_]: Concurrent](
      stream: fs2.Stream[F, Byte]
  ): F[Blob] = stream.chunks.compile
    .to(fs2.Chunk)
    .map(_.flatten)
    .map(chunk => Blob(chunk.toArray))

  private def getHeaders[F[_]](req: Media[F]) =
    req.headers.headers.groupBy(_.name).map { case (k, v) =>
      (CaseInsensitive(k.toString), v.map(_.value))
    }

  private def toStream[F[_]](
      blob: Blob
  ): fs2.Stream[F, Byte] =
    // Optimisation motivated by https://github.com/http4s/http4s/issues/7539
    if (blob.isEmpty) fs2.Stream.empty else fs2.Stream.chunk(fs2.Chunk.array(blob.toArray))
}
