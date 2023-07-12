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

        val stringAndBlobsEntityEncoders =
          smithy4s.http.StringAndBlobCodecs.WriterCompiler
            .mapK(
              Covariant.liftPolyFunction[Option](
                EntityEncoders.fromHttpMediaWriterK[F]
              )
            )

        val stringAndBlobsEntityDecoders =
          smithy4s.http.StringAndBlobCodecs.ReaderCompiler
            .mapK(
              Covariant.liftPolyFunction[Option](
                EntityDecoders.fromHttpMediaReaderK[F]
              )
            )

        val xmlEntityEncoders: CachedSchemaCompiler[EntityEncoder[F, *]] =
          XmlDocument.Encoder.mapK(
            new PolyFunction[XmlDocument.Encoder, EntityEncoder[F, *]] {
              def apply[A](fa: XmlDocument.Encoder[A]): EntityEncoder[F, A] =
                xmlEntityEncoder[F].contramap(fa.encode)
            }
          )

        val encoders = CachedSchemaCompiler.getOrElse(
          stringAndBlobsEntityEncoders,
          xmlEntityEncoders
        )

        val xmlEntityDecoders: CachedSchemaCompiler[EntityDecoder[F, *]] =
          XmlDocument.Decoder.mapK(
            new PolyFunction[XmlDocument.Decoder, EntityDecoder[F, *]] {
              def apply[A](
                  fa: XmlDocument.Decoder[A]
              ): EntityDecoder[F, A] =
                xmlEntityDecoder[F].flatMapR(xmlDocument =>
                  EitherT.liftF {
                    fa.decode(xmlDocument)
                      .leftMap(fromXmlToHttpError)
                      .liftTo[F]
                  }
                )
            }
          )

        val decoders = CachedSchemaCompiler.getOrElse(
          stringAndBlobsEntityDecoders,
          xmlEntityDecoders
        )

        val restEncoders =
          RequestEncoder.restSchemaCompiler[F](Metadata.AwsEncoder, encoders)

        val restDecoders =
          ResponseDecoder.restSchemaCompiler(Metadata.AwsDecoder, decoders)

        val errorDecoders = restDecoders.contramapSchema(
          smithy4s.schema.Schema.transformHintsLocallyK(
            _ ++ smithy4s.Hints(
              smithy4s.xml.internals.XmlStartingPath(
                List("ErrorResponse", "Error")
              )
            )
          )
        )

        val discriminator = AwsErrorTypeDecoder.fromResponse(errorDecoders)

        val transformEncoders = applyCompression[F](endpoint.hints)
        val finalEncoders = transformEncoders(restEncoders)

        val make =
          UnaryClientCodecs
            .Make[F](finalEncoders, restDecoders, errorDecoders, discriminator)
        make.apply(endpoint)
      }
    }

  private def xmlEntityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, XmlDocument] =
    EntityDecoder.decodeBy(
      MediaRange.parse("application/xml").getOrElse(sys.error("TODO"))
    ) { media =>
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
    }

  private def xmlEntityEncoder[F[_]: Applicative]
      : EntityEncoder[F, XmlDocument] =
    EntityEncoder.encodeBy(
      org.http4s.headers.`Content-Type`.apply(MediaType.application.xml)
    ) { xmlDocument =>
      val body: fs2.Chunk[Byte] = XmlDocument.documentEventifier
        .eventify(xmlDocument)
        .through(render())
        .through(fs2.text.utf8.encode[fs2.Pure])
        .compile
        .foldChunks(fs2.Chunk.empty[Byte])(_ ++ _)

      org.http4s.Entity.apply(
        fs2.Stream.chunk(body),
        Some(body.size.toLong)
      )
    }

  private def fromXmlToHttpError(
      xmlDecodeError: smithy4s.xml.XmlDecodeError
  ): smithy4s.http.HttpContractError = {
    smithy4s.http.HttpPayloadError(
      xmlDecodeError.path.toPayloadPath,
      "",
      xmlDecodeError.message
    )
  }

}
