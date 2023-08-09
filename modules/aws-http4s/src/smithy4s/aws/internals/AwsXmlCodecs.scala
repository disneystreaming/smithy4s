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

package smithy4s.aws
package internals

import cats.Applicative
import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all._
import fs2.compression.Compression
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import org.http4s.MediaRange
import org.http4s.MediaType
import smithy4s.Endpoint
import smithy4s.capability.Covariant
import smithy4s.http.Metadata
import smithy4s.http4s.kernel._
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.xml.Xml
import smithy4s.fs2._

private[aws] object AwsXmlCodecs {

  def make[F[_]: Concurrent: Compression](): UnaryClientCodecs.Make[F] =
    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val transformEncoders = applyCompression[F](endpoint.hints)
        val requestEncoderCompilersWithCompression = transformEncoders(
          requestEncoderCompilers[F]
        )

        val errorDecoderCompilers = responseDecoderCompilers[F].contramapSchema(
          smithy4s.schema.Schema.transformHintsLocallyK(
            _ ++ smithy4s.Hints(
              smithy4s.xml.internals.XmlStartingPath(
                List("ErrorResponse", "Error")
              )
            )
          )
        )
        val errorDiscriminator =
          AwsErrorTypeDecoder.fromResponse(errorDecoderCompilers)

        val make = UnaryClientCodecs.Make[F](
          input = requestEncoderCompilersWithCompression,
          output = responseDecoderCompilers[F],
          error = errorDecoderCompilers,
          errorDiscriminator = errorDiscriminator
        )
        make.apply(endpoint)
      }
    }

  private def requestEncoderCompilers[F[_]: Concurrent]
      : CachedSchemaCompiler[RequestEncoder[F, *]] = {
    val stringAndBlobsEntityEncoderCompilers =
      smithy4s.http.StringAndBlobCodecs.WriterCompiler.mapK(
        Covariant.liftPolyFunction[Option](
          EntityEncoders.fromHttpMediaWriterK[F]
        )
      )
    val xmlEntityEncoderCompilers = xmlEntityEncoder[F]
    val entityEncoderCompilers = CachedSchemaCompiler.getOrElse(
      stringAndBlobsEntityEncoderCompilers,
      xmlEntityEncoderCompilers
    )
    RequestEncoder.restSchemaCompiler[F](
      metadataEncoderCompiler = Metadata.AwsEncoder,
      entityEncoderCompiler = entityEncoderCompilers
    )
  }

  def responseDecoderCompilers[F[_]: Concurrent]
      : CachedSchemaCompiler[ResponseDecoder[F, *]] = {
    val stringAndBlobsEntityDecoderCompilers =
      smithy4s.http.StringAndBlobCodecs.ReaderCompiler.mapK(
        Covariant.liftPolyFunction[Option](
          EntityDecoders.fromHttpMediaReaderK[F]
        )
      )
    val xmlEntityDecoderCompilers = xmlEntityDecoder[F]

    val entityDecoderCompilers = CachedSchemaCompiler.getOrElse(
      stringAndBlobsEntityDecoderCompilers,
      xmlEntityDecoderCompilers
    )
    ResponseDecoder.restSchemaCompiler(
      metadataDecoderCompiler = Metadata.AwsDecoder,
      entityDecoderCompiler = entityDecoderCompilers
    )
  }

  private def xmlEntityEncoder[F[_]: Applicative]
      : CachedSchemaCompiler[EntityEncoder[F, *]] =
    Xml.xmlByteStreamEncoders[fs2.Pure].mapK {
      new PolyFunction[
        XmlByteStreamEncoder[fs2.Pure, *],
        EntityEncoder[F, *]
      ] {
        def apply[A](
            encoder: XmlByteStreamEncoder[fs2.Pure, A]
        ): EntityEncoder[F, A] =
          EntityEncoder.encodeBy(
            org.http4s.headers.`Content-Type`(MediaType.application.xml)
          ) { xmlDocument =>
            val body = encoder
              .encode(xmlDocument)
              .compile
              .foldChunks(fs2.Chunk.empty[Byte])(_ ++ _)
            org.http4s.Entity(
              body = fs2.Stream.chunk(body),
              length = Some(body.size.toLong)
            )
          }
      }
    }

  private def xmlEntityDecoder[F[_]: Concurrent]
      : CachedSchemaCompiler[EntityDecoder[F, *]] =
    Xml.xmlByteStreamDecoders[F].mapK {
      val xmlMediaRange = MediaRange
        .parse("application/xml")
        .getOrElse(throw new RuntimeException("Unable to parse xml MediaRange"))
      new PolyFunction[XmlByteStreamDecoder[F, *], EntityDecoder[F, *]] {
        def apply[A](
            decoder: XmlByteStreamDecoder[F, A]
        ): EntityDecoder[F, A] =
          EntityDecoder.decodeBy(
            xmlMediaRange
          )(media =>
            EitherT.liftF(
              decoder.decode(media.body).adaptError(fromXmlToHttpError)
            )
          )
      }
    }

  private val fromXmlToHttpError: PartialFunction[Throwable, Throwable] = {
    case xmlDecodeError: smithy4s.xml.XmlDecodeError =>
      smithy4s.http.HttpPayloadError(
        xmlDecodeError.path.toPayloadPath,
        "",
        xmlDecodeError.message
      )
  }

}
