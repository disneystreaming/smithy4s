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

package smithy4s.http4s.kernel

import cats.effect.Concurrent
import org.http4s.Entity
import org.http4s.EntityEncoder
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Response
import org.http4s.Status
import org.typelevel.ci.CIString
import smithy4s.Errorable
import smithy4s.codecs.Writer
import smithy4s.codecs.Encoder
import smithy4s.http.HttpStatusCode
import smithy4s.http._
import smithy4s.kinds.PolyFunction
import smithy4s.schema.Alt
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema

object ResponseEncoder {

  type CachedCompiler[F[_]] = CachedSchemaCompiler[ResponseEncoder[F, *]]

  def forError[F[_], E](
      errorTypeHeaders: List[String],
      maybeErrorable: Option[Errorable[E]],
      encoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ResponseEncoder[F, E] = maybeErrorable match {
    case Some(errorable) =>
      forErrorAux(errorTypeHeaders, errorable, encoderCompiler)
    case None => Writer.noop
  }

  private def forErrorAux[F[_], E](
      errorTypeHeaders: List[String],
      errorable: Errorable[E],
      encoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ResponseEncoder[F, E] = {
    val errorUnionSchema = errorable.error
    val dispatcher =
      Alt.Dispatcher(errorUnionSchema.alternatives, errorUnionSchema.ordinal)
    val precompiler = new Alt.Precompiler[ResponseEncoder[F, *]] {
      def apply[Err](
          label: String,
          errorSchema: Schema[Err]
      ): ResponseEncoder[F, Err] = new ResponseEncoder[F, Err] {
        val errorEncoder =
          encoderCompiler.fromSchema(errorSchema, encoderCompiler.createCache())
        def write(response: Response[F], err: Err): Response[F] = {
          val errorCode =
            HttpStatusCode.fromSchema(errorSchema).code(err, 500)
          val status =
            Status.fromInt(errorCode).getOrElse(Status.InternalServerError)
          val encodedResponse = errorEncoder.write(response, err)
          encodedResponse
            .withStatus(status)
            .putHeaders(errorTypeHeaders.map(_ -> label))
        }
      }
    }
    dispatcher.compile(precompiler)
  }

  def metadataResponseEncoder[F[_]]: ResponseEncoder[F, Metadata] =
    new ResponseEncoder[F, Metadata] {
      def write(response: Response[F], metadata: Metadata): Response[F] = {
        val headers = toHeaders(metadata.headers)
        val status = metadata.statusCode
          .flatMap(Status.fromInt(_).toOption)
          .filter(_.responseClass.isSuccess)
          .getOrElse(response.status)
        response.withHeaders(response.headers ++ headers).withStatus(status)
      }
    }

  def fromMetadataEncoder[F[_], A](
      metadataEncoder: Metadata.Encoder[A]
  ): ResponseEncoder[F, A] =
    metadataResponseEncoder[F].contramap(metadataEncoder.encode)

  def fromMetadataEncoderK[F[_]]
      : PolyFunction[Metadata.Encoder, ResponseEncoder[F, *]] =
    new PolyFunction[Metadata.Encoder, ResponseEncoder[F, *]] {
      def apply[A](fa: Metadata.Encoder[A]): ResponseEncoder[F, A] =
        fromMetadataEncoder[F, A](fa)
    }

  def fromEntityEncoder[F[_]: Concurrent, A](implicit
      entityEncoder: EntityEncoder[F, A]
  ): ResponseEncoder[F, A] = new ResponseEncoder[F, A] {
    def write(response: Response[F], a: A): Response[F] = {
      response.withEntity(a)
    }
  }

  def fromEntityEncoderK[F[_]: Concurrent]
      : PolyFunction[EntityEncoder[F, *], ResponseEncoder[F, *]] =
    new PolyFunction[EntityEncoder[F, *], ResponseEncoder[F, *]] {
      def apply[A](fa: EntityEncoder[F, A]): ResponseEncoder[F, A] =
        fromEntityEncoder[F, A](Concurrent[F], fa)
    }

  def fromEntityEncoder2[F[_]: Concurrent, A](implicit
      entityEncoder: EntityEncoder[F, A]
  ): Encoder[Entity[F], A] = new Encoder[Entity[F], A] {
    def write(any: Any, a: A): Entity[F] = {
      entityEncoder.toEntity(a)
    }
  }

  def fromEntityEncoderK2[F[_]: Concurrent]
      : PolyFunction[EntityEncoder[F, *], Encoder[Entity[F], *]] =
    new PolyFunction[EntityEncoder[F, *], Encoder[Entity[F], *]] {
      def apply[A](fa: EntityEncoder[F, A]): Encoder[Entity[F], A] =
        fromEntityEncoder2[F, A](Concurrent[F], fa)
    }

  private def fromHttpResponse[F[_]](
      res: HttpResponse[Entity[F]]
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

  private def fromResponse[F[_]](
      res: Response[F]
  ): HttpResponse[Entity[F]] = HttpResponse[Entity[F]](
    res.status.code,
    res.headers.headers
      .map(h => CaseInsensitive(h.name.toString) -> Seq(h.value))
      .toMap,
    Entity(
      res.body,
      res.headers.get[org.http4s.headers.`Content-Length`].map(_.length)
    )
  )

  private def toResponseEncoder[F[_]]: PolyFunction[
    HttpResponse.Encoder[Entity[F], *],
    ResponseEncoder[F, *]
  ] = new PolyFunction[
    HttpResponse.Encoder[Entity[F], *],
    ResponseEncoder[F, *]
  ] {
    def apply[A](
        encoder: HttpResponse.Encoder[Entity[F], A]
    ): ResponseEncoder[F, A] = new ResponseEncoder[F, A]() {
      def write(input: Response[F], a: A): Response[F] = {
        fromHttpResponse(encoder.write(fromResponse(input), a))
      }
    }
  }

  /**
    * A compiler for ResponseEncoder that encodes the whole data in the body
    * of the request
    */
  def rpcSchemaCompiler[F[_]](
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[ResponseEncoder[F, *]] =
    entityEncoderCompiler.mapK(fromEntityEncoderK[F])

  /**
    * A compiler for ResponseEncoder that abides by REST-semantics :
    * fields that are annotated with `httpHeader` and `httpStatusCode`
    * are encoded as the corresponding metadata.
    *
    * The rest is used to formulate the body of the message.
    */
  def restSchemaCompiler[F[_]](
      metadataEncoderCompiler: CachedSchemaCompiler[Metadata.Encoder],
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[ResponseEncoder[F, *]] = {
    val bodyCompiler =
      entityEncoderCompiler.mapK(fromEntityEncoderK2)
    HttpResponse.Encoder
      .restSchemaCompiler[Entity[F]](metadataEncoderCompiler, bodyCompiler)
      .mapK(toResponseEncoder[F])
  }

}
