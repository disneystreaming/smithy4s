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

import smithy.api.XmlName
import cats.effect.Concurrent
import cats.syntax.all._
import fs2.compression.Compression
import smithy4s.Endpoint
import smithy4s.http._
import smithy4s.http4s.kernel._
import _root_.aws.protocols.Ec2QueryName
import _root_.aws.protocols.AwsQueryError

private[aws] object AwsEcsQueryCodecs {

  def make[F[_]: Concurrent: Compression](
      version: String
  ): UnaryClientCodecs.Make[F] =
    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val requestEncoderCompilers = AwsQueryCodecs
          .requestEncoderCompilers[F](
            ignoreXmlFlattened = true,
            capitalizeStructAndUnionMemberNames = true,
            action = endpoint.id.name,
            version = version
          )
          .contramapSchema(
            smithy4s.schema.Schema.transformHintsTransitivelyK(hints =>
              hints.get(Ec2QueryName) match {
                case Some(ec2QueryName) =>
                  hints.filterNot(_.keyId == XmlName.id) ++ smithy4s.Hints(
                    XmlName(ec2QueryName.value)
                  )

                case None =>
                  hints.get(XmlName) match {
                    case Some(xmlName) =>
                      hints.filterNot(_.keyId == XmlName.id) ++ smithy4s.Hints(
                        XmlName(xmlName.value.capitalize)
                      )

                    case _ =>
                      hints
                  }
              }
            )
          )
        val transformEncoders =
          applyCompression[F](endpoint.hints, retainUserEncoding = false)
        val requestEncoderCompilersWithCompression = transformEncoders(
          requestEncoderCompilers
        )

        val responseTag = endpoint.name + "Response"
        val responseDecoderCompilers =
          AwsXmlCodecs
            .responseDecoderCompilers[F]
            .contramapSchema(
              smithy4s.schema.Schema.transformHintsLocallyK(
                _ ++ smithy4s.Hints(
                  smithy4s.xml.internals.XmlStartingPath(
                    List(responseTag)
                  )
                )
              )
            )
        val errorDecoderCompilers = AwsXmlCodecs
          .responseDecoderCompilers[F]
          .contramapSchema(
            smithy4s.schema.Schema.transformHintsLocallyK(
              _ ++ smithy4s.Hints(
                smithy4s.xml.internals.XmlStartingPath(
                  List("Response", "Errors", "Error")
                )
              )
            )
          )

        // Takes the `@awsQueryError` trait into consideration to decide how to
        // discriminate error responses.
        val errorNameMapping: (String => String) = endpoint.errorable match {
          case None =>
            identity[String]

          case Some(err) =>
            val mapping = err.error.alternatives.flatMap { alt =>
              val shapeName = alt.schema.shapeId.name
              alt.hints.get(AwsQueryError).map(_.code).map(_ -> shapeName)
            }.toMap
            (errorCode: String) => mapping.getOrElse(errorCode, errorCode)
        }
        val errorDiscriminator = AwsErrorTypeDecoder
          .fromResponse(errorDecoderCompilers)
          .andThen(_.map(_.map {
            case HttpDiscriminator.NameOnly(name) =>
              HttpDiscriminator.NameOnly(errorNameMapping(name))
            case other => other
          }))

        val make = UnaryClientCodecs.Make[F](
          input = requestEncoderCompilersWithCompression,
          output = responseDecoderCompilers,
          error = errorDecoderCompilers,
          errorDiscriminator = errorDiscriminator
        )
        make.apply(endpoint)
      }
    }

}
