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

package smithy4s.http

import smithy4s.codecs.{Reader, Writer}
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.{Encoder => BodyEncoder}
import smithy4s.capability.Zipper

final case class HttpResponse[+A](
    statusCode: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: A
) {
  def withStatusCode(statusCode: Int): HttpResponse[A] =
    this.copy(statusCode = statusCode)
  def withHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpResponse[A] =
    this.copy(headers = headers)
  def addHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpResponse[A] =
    this.copy(headers = this.headers ++ headers)
}

object HttpResponse {
  type Encoder[Body, A] = Writer[HttpResponse[Body], HttpResponse[Body], A]
  type Decoder[F[_], Body, A] = Reader[F, HttpRequest[Body], A]

  object Encoder {
    private def isStatusCodeSuccess(statusCode: Int): Boolean =
      100 <= statusCode && statusCode <= 399

    private def metadataEncoder[Body]: Encoder[Body, Metadata] = {
      (resp: HttpResponse[Body], meta: Metadata) =>
        val statusCode =
          meta.statusCode
            .filter(isStatusCodeSuccess)
            .getOrElse(resp.statusCode)
        resp
          .withStatusCode(statusCode)
          .addHeaders(meta.headers)
    }

    private def bodyEncoder[Body]: Encoder[Body, Body] = {
      (resp: HttpResponse[Body], body: Body) => resp.copy(body = body)
    }

    private def fromMetadataEncoderK[Body]
        : PolyFunction[Metadata.Encoder, Encoder[Body, *]] =
      Metadata.Encoder.toWriterK
        .widen[Writer[HttpResponse[Body], Metadata, *]]
        .andThen(Writer.pipeDataK(metadataEncoder[Body]))

    private def fromEntityEncoderK[Body]
        : PolyFunction[BodyEncoder[Body, *], Encoder[Body, *]] =
      Writer.pipeDataK[HttpResponse[Body], Body](bodyEncoder[Body]).narrow

    def restSchemaCompiler[Body](
        metadataEncoderCompiler: CachedSchemaCompiler[Metadata.Encoder],
        bodyEncoderCompiler: CachedSchemaCompiler[BodyEncoder[Body, *]]
    ): CachedSchemaCompiler[Encoder[Body, *]] = {
      val metadataCompiler =
        metadataEncoderCompiler.mapK(fromMetadataEncoderK[Body])
      val bodyCompiler =
        bodyEncoderCompiler.mapK(fromEntityEncoderK[Body])
      HttpRestSchema.combineWriterCompilers(metadataCompiler, bodyCompiler)
    }
  }

  object Decoder {

    private def extractMetadata[F[_]](
        liftToF: PolyFunction[Either[MetadataError, *], F]
    ): PolyFunction[Metadata.Reader, Decoder[F, Any, *]] =
      Reader
        .in[Either[MetadataError, *]]
        .composeK((_: HttpRequest[Any]).toMetadata)
        .andThen(Reader.liftPolyFunction(liftToF))

    private def extractBody[F[_], Body]
        : PolyFunction[Reader[F, Body, *], Decoder[F, Body, *]] =
      Reader.in[F].composeK(_.body)

    def restSchemaCompiler[F[_]: Zipper, Body](
        metadataDecoderCompiler: CachedSchemaCompiler[Metadata.Decoder],
        entityDecoderCompiler: CachedSchemaCompiler[Reader[F, Body, *]],
        liftToF: PolyFunction[Either[MetadataError, *], F]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
      val restMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
        metadataDecoderCompiler.mapK(
          Metadata.Decoder.toReaderK.andThen(extractMetadata[F](liftToF))
        )

      val bodyMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
        entityDecoderCompiler.mapK { extractBody[F, Body] }

      HttpRestSchema.combineReaderCompilers[F, HttpRequest[Body]](
        restMetadataCompiler,
        bodyMetadataCompiler
      )
    }

  }

}
