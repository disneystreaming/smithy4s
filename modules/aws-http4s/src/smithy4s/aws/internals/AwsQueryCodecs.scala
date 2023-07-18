/*
 *  Copyright 2023 Disney Streaming
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
import fs2.compression.Compression
import smithy4s.Endpoint
import smithy4s.schema.CachedSchemaCompiler
import org.http4s.EntityDecoder
import smithy4s.xml.XmlDocument
import org.http4s.MediaRange
import fs2.data.xml._
import fs2.data.xml.dom._
// import smithy4s.codecs._
import smithy4s.http._
// import smithy4s.http.internals._
import smithy4s.http4s.kernel._
import cats.data.EitherT
import smithy4s.kinds.PolyFunction
import org.http4s.EntityEncoder
import smithy4s.capability.Covariant

import smithy4s.http.Metadata

private[aws] object AwsQueryCodecs {

  def make[F[_]: Concurrent: Compression](): UnaryClientCodecs.Make[F] =
    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {

        val stringAndBlobsEntityDecoders =
          smithy4s.http.StringAndBlobCodecs.ReaderCompiler
            .mapK(
              Covariant.liftPolyFunction[Option](
                EntityDecoders.fromHttpMediaReaderK[F]
              )
            )

        val urlFormEntityEncoders: CachedSchemaCompiler[EntityEncoder[F, *]] =
          UrlForm.Encoder.mapK(
            new PolyFunction[UrlForm.Encoder, EntityEncoder[F, *]] {
              def apply[A](fa: UrlForm.Encoder[A]): EntityEncoder[F, A] =
                urlFormEntityEncoder[F].contramap(fa.encode)
            }
          )

        // TODO: Not needed here, but will be needed for an OAuth server
        // val urlFormEntityDecoders: CachedSchemaCompiler[EntityDecoder[F, *]] =
        //   UrlForm.Decoder.mapK(
        //     new PolyFunction[UrlForm.Decoder, EntityDecoder[F, *]] {
        //       def apply[A](
        //           fa: UrlForm.Decoder[A]
        //       ): EntityDecoder[F, A] =
        //         urlFormEntityDecoder[F].flatMapR(urlForm =>
        //           EitherT.liftF {
        //             fa.decode(urlForm)
        //               .leftMap(fromUrlFormToHttpError)
        //               .liftTo[F]
        //           }
        //         )
        //     }
        //   )

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
          RequestEncoder.restSchemaCompiler[F](Metadata.AwsEncoder, urlFormEntityEncoders)

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

  // TODO: Not needed here, but will be needed for an OAuth server
  // private def urlFormEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, UrlForm] = EntityDecoders.fromHttpMediaReader(
  //   HttpMediaTyped(
  //     HttpMediaType("application/x-www-form-urlencoded"),
  //       // TODO: Avoid going to string and back
  //       // TODO: Make error mapping consistent with elsewhere
  //       blob => UrlFormParser.parseUrlForm(blob.toUTF8String).left.map(parseFailure =>
  //         HttpPayloadError(
  //           PayloadPath.root,
  //           "",
  //           parseFailure.message
  //         )
  //       )
  //   )
  // )

  private def urlFormEntityEncoder[F[_]: Concurrent]: EntityEncoder[F, UrlForm] = EntityEncoders.fromHttpMediaWriter(
    HttpMediaTyped(
      HttpMediaType("application/x-www-form-urlencoded"),
        (_: Any, urlForm: UrlForm) => Blob(urlForm.values.render)
    )
  )

  private def fromXmlToHttpError(
      xmlDecodeError: smithy4s.xml.XmlDecodeError
  ): smithy4s.http.HttpContractError = {
    smithy4s.http.HttpPayloadError(
      xmlDecodeError.path.toPayloadPath,
      "",
      xmlDecodeError.message
    )
  }

  // TODO: Not needed here, but will be needed for an OAuth server
  // private def fromUrlFormToHttpError(
  //     urlFormDecodeError: smithy4s.http.UrlFormDecodeError
  // ): smithy4s.http.HttpContractError = {
  //   smithy4s.http.HttpPayloadError(
  //     urlFormDecodeError.path,
  //     "",
  //     urlFormDecodeError.message
  //   )
  // }

}
