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
package aws
package internals

import cats.effect.Concurrent
import cats.syntax.all._
import cats.Applicative
import fs2.compression.Compression
import smithy4s.http4s.kernel._
import smithy4s.Endpoint
import smithy4s.schema.CachedSchemaCompiler
import org.http4s.EntityDecoder
import smithy4s.xml.XmlDocument
import org.http4s.MediaRange
import fs2.data.xml._
import fs2.data.xml.dom._
import cats.data.EitherT
import smithy4s.kinds.PolyFunction
import org.http4s.EntityEncoder
import smithy4s.capability.Covariant

import org.http4s.MediaType
import smithy4s.http.Metadata

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
    val xmlEntityEncoderCompilers = XmlDocument.Encoder.mapK(
      new PolyFunction[XmlDocument.Encoder, EntityEncoder[F, *]] {
        def apply[A](fa: XmlDocument.Encoder[A]): EntityEncoder[F, A] =
          xmlEntityEncoder[F].contramap(fa.encode)
      }
    )
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
    val xmlEntityDecoderCompilers = XmlDocument.Decoder.mapK(
      new PolyFunction[XmlDocument.Decoder, EntityDecoder[F, *]] {
        def apply[A](fa: XmlDocument.Decoder[A]): EntityDecoder[F, A] =
          xmlEntityDecoder[F].flatMapR(xmlDocument =>
            EitherT.liftF {
              fa.decode(xmlDocument)
                .leftMap(fromXmlToHttpError)
                .liftTo[F]
            }
          )
      }
    )
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
      : EntityEncoder[F, XmlDocument] =
    EntityEncoder.encodeBy(
      org.http4s.headers.`Content-Type`(MediaType.application.xml)
    ) { xmlDocument =>
      val body: fs2.Chunk[Byte] = XmlDocument.documentEventifier
        .eventify(xmlDocument)
        .through(render(collapseEmpty = false))
        .through(fs2.text.utf8.encode[fs2.Pure])
        .compile
        .foldChunks(fs2.Chunk.empty[Byte])(_ ++ _)
      org.http4s.Entity(
        body = fs2.Stream.chunk(body),
        length = Some(body.size.toLong)
      )
    }

  private def xmlEntityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, XmlDocument] =
    EntityDecoder.decodeBy(
      MediaRange.parse("application/xml").getOrElse(sys.error("TODO"))
    )(media =>
      EitherT.liftF(
        media.body
          .through(fs2.text.utf8.decode[F])
          .through(events[F, String]())
          .through(referenceResolver())
          .through(normalize)
          .through(documents[F, XmlDocument])
          .head
          .compile
          .last
          .map(
            _.getOrElse(
              // TODO: This isn't right
              XmlDocument(
                XmlDocument.XmlElem(
                  XmlDocument.XmlQName(None, "Unit"),
                  List.empty,
                  List.empty
                )
              )
            )
          )
      )
    )

  private def fromXmlToHttpError(
      xmlDecodeError: smithy4s.xml.XmlDecodeError
  ): smithy4s.http.HttpContractError =
    smithy4s.http.HttpPayloadError(
      xmlDecodeError.path.toPayloadPath,
      "",
      xmlDecodeError.message
    )

}
