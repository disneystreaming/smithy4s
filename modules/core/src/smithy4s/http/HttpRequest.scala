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

import smithy4s.kinds._
import smithy4s.codecs._
import smithy4s.capability.Covariant
import smithy4s.capability.Zipper
import smithy4s.schema._

final case class HttpUri(
    host: String,
    path: Seq[String],
    queryParams: Map[String, Seq[String]]
)

final case class HttpRequest[+A](
    uri: HttpUri,
    headers: Map[CaseInsensitive, Seq[String]],
    body: A,
    pathParams: Option[Map[String, String]] // TODO docs
) {
  def map[B](f: A => B): HttpRequest[B] =
    HttpRequest(uri, headers, f(body), pathParams)

  def toMetadata: Metadata = Metadata(
    path = pathParams.getOrElse(Map.empty),
    query = uri.queryParams,
    headers = headers
  )
}

object HttpRequest {
  type Decoder[F[_], Body, A] = Reader[F, HttpRequest[Body], A]
  type BodyDecoder[F[_], Body, A] = Reader[F, Body, A]

  implicit val reqCovariant: Covariant[HttpRequest] =
    new Covariant[HttpRequest] {
      def map[A, B](req: HttpRequest[A])(f: A => B): HttpRequest[B] = req.map(f)
    }

  def fromMetadataDecoder[A](
      metadataDecoder: Metadata.Decoder[A]
  ): Decoder[Either[MetadataError, *], Any, A] =
    new Decoder[Either[MetadataError, *], Any, A] {
      def read(request: HttpRequest[Any]): Either[MetadataError, A] = {
        metadataDecoder.decode(request.toMetadata)
      }
    }

  private type MetadataReader[A] = Reader[Either[MetadataError, *], Metadata, A]
  private def extractMetadata[F[_], Body](
      liftToF: PolyFunction[Either[MetadataError, *], F]
  ): PolyFunction[MetadataReader, Decoder[F, Body, *]] =
    new PolyFunction[MetadataReader, Decoder[F, Body, *]] {
      def apply[A](mr: MetadataReader[A]): Decoder[F, Body, A] = {
        mr.compose[HttpRequest[Body]](_.toMetadata).mapK(liftToF)
      }
    }

  private def extractBody[F[_], Body]
      : PolyFunction[Reader[F, Body, *], Decoder[F, Body, *]] =
    new PolyFunction[Reader[F, Body, *], Decoder[F, Body, *]] {
      def apply[A](mr: Reader[F, Body, A]): Decoder[F, Body, A] = {
        mr.compose[HttpRequest[Body]](_.body)
      }
    }

  def restSchemaCompiler[F[_]: Zipper, Body](
      metadataDecoderCompiler: CachedSchemaCompiler[Metadata.Decoder],
      entityDecoderCompiler: CachedSchemaCompiler[BodyDecoder[F, Body, *]],
      liftToF: PolyFunction[Either[MetadataError, *], F]
  ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
    val restMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
      metadataDecoderCompiler.mapK(
        Metadata.Decoder.toReaderK
          .andThen(extractMetadata[F, Body](liftToF))
      )

    val bodyMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
      entityDecoderCompiler.mapK { extractBody[F, Body] }

    HttpRestSchema.combineReaderCompilers[F, HttpRequest[Body]](
      restMetadataCompiler,
      bodyMetadataCompiler
    )
  }
}
