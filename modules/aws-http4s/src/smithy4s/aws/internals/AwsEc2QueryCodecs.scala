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

import _root_.aws.protocols.AwsQueryError
import _root_.aws.protocols.Ec2QueryName
import cats.effect.Concurrent
import cats.syntax.all._
import fs2.compression.Compression
import smithy.api.XmlName
import smithy4s.Endpoint
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.xml.internals.XmlStartingPath

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
            // These are set to fulfil the requirements of
            // https://smithy.io/2.0/aws/protocols/aws-ec2-query-protocol.html?highlight=ec2%20query%20protocol#query-key-resolution.
            // without UrlFormDataEncoderSchemaVisitor having to be more aware
            // than necessary of these protocol quirks.
            ignoreUrlFormFlattened = true,
            capitalizeStructAndUnionMemberNames = true,
            action = endpoint.id.name,
            version = version
          )
          .contramapSchema(
            // This pre-processing works in collaboration with the passing of
            // the capitalizeStructAndUnionMemberNames flags to
            // UrlFormDataEncoderSchemaVisitor.
            Schema.transformHintsTransitivelyK(hints =>
              hints.memberHints.get(Ec2QueryName) match {
                case Some(ec2QueryName) =>
                  hints.addMemberHints(XmlName(ec2QueryName.value))

                case None =>
                  hints.memberHints.get(XmlName) match {
                    case Some(xmlName) =>
                      hints.addMemberHints(XmlName(xmlName.value.capitalize))
                    case None => hints
                  }
              }
            )
          )
        val transformEncoders = applyCompression[F](
          endpoint.hints,
          // To fulfil the requirement of
          // https://github.com/smithy-lang/smithy/blob/main/smithy-aws-protocol-tests/model/ec2Query/requestCompression.smithy#L152-L298.
          retainUserEncoding = false
        )
        val requestEncoderCompilersWithCompression = transformEncoders(
          requestEncoderCompilers
        )

        val responseTag = endpoint.name + "Response"
        val responseDecoderCompilers =
          AwsXmlCodecs
            .responseDecoderCompilers[F]
            .contramapSchema(
              Schema.transformHintsLocallyK(
                _ ++ Hints(XmlStartingPath(List(responseTag)))
              )
            )
        val errorDecoderCompilers = AwsXmlCodecs
          .responseDecoderCompilers[F]
          .contramapSchema(
            Schema.transformHintsLocallyK(
              _ ++ Hints(XmlStartingPath(List("Response", "Errors", "Error")))
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
            errorCode => mapping.getOrElse(errorCode, errorCode)
        }
        val errorDiscriminator = AwsErrorTypeDecoder
          .fromResponse(errorDecoderCompilers)
          .andThen(_.map(_.map {
            case HttpDiscriminator.NameOnly(name) =>
              HttpDiscriminator.NameOnly(errorNameMapping(name))
            case other =>
              other
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
