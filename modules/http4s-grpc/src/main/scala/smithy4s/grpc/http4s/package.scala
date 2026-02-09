package smithy4s.grpc

import cats.effect.Concurrent
import cats.implicits._
import org.http4s.Request
import org.http4s.Response
import smithy4s.Blob
import org.http4s.Status
import org.http4s.Headers
import org.http4s.Uri
import smithy4s.grpc.GrpcStatus
import smithy4s.grpc.GrpcResponse
import org.http4s.Media
import smithy4s.http.{HttpUri => Smithy4sHttpUri}
import smithy4s.http.{HttpUriScheme => Smithy4sHttpUriScheme}
import cats.MonadThrow
import org.http4s.Header
import org.typelevel.ci.CIString
import org.http4s.Method
import org.http4s.HttpVersion

package object http4s {

  type PathParams = Map[String, String]

  def fromGrpcRequest[F[_]: MonadThrow](
      req: GrpcRequest[Blob],
      encodePathSegments: Boolean
  ): Request[F] = {
    val headers = toHeaders(req.headers)
    val updatedHeaders = req.body.size match {
      case 0             => headers
      case contentLength => headers.put("Content-Length" -> contentLength.toString)
    }
    Request(
      Method.POST,
      fromSmithy4sHttpUri(req.uri, encodePathSegments = encodePathSegments),
      headers = updatedHeaders,
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

  def fromGrpcResponse[F[_]: Concurrent](res: GrpcResponse[Blob]): Response[F] = {
    val headers = toHeaders(res.headers)
    val updatedHeaders = {
      val contentLength = res.body.size
      if (contentLength <= 0) headers
      else headers.put("Content-Length" -> contentLength.toString)
    }

    val trailerHeaders = Concurrent[F].pure(
      Headers(
        GrpcHeaders.grpcStatusHeader.name.toString ->  GrpcHeaders.grpcStatusHeader.value(res.status)
      )
    )
    Response(Status.Ok, headers = updatedHeaders, body = toStream(res.body), httpVersion = HttpVersion.`HTTP/2`)
      .withTrailerHeaders(trailerHeaders)
  }

  def toGrpcResponse[F[_]](res: Response[F])(implicit F: Concurrent[F]): F[GrpcResponse[Blob]] = {
    import smithy4s.grpc.http4s.GrpcHeaders.grpcStatusHeader
    // implicit val foo = implicitly[org.http4s.Header.Select[GrpcStatus]]
    for {
      blob <- collectBytes(res.body)
      httpStatus <- F.pure(res.status)
      grpcStatus <- httpStatus match {
        case s if s.isSuccess =>
          res.trailerHeaders.map(_.get[GrpcStatus]).flatMap{
            case Some(header) => F.pure(header)
            case None => F.pure[GrpcStatus](GrpcStatus.Ok) //FIXME: is it ok to default to GrpcStatus.Ok?
          }
        case s => 
          F.raiseError[GrpcStatus](new RuntimeException(s"HTTP/2 transport failed with ${s}"))
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
