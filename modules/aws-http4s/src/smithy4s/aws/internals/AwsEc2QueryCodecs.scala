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

import _root_.aws.protocols.Ec2QueryName
import alloy.UrlFormName
import smithy.api.XmlName
import smithy4s._
import smithy4s.capability.Covariant
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.Writer
import smithy4s.http._
import smithy4s.interopcats._
import smithy4s.kinds.PolyFunction5
import smithy4s.xml.Xml
import smithy4s.xml.internals.XmlStartingPath

// scalafmt: { maxColumn = 120}
private[aws] object AwsEcsQueryCodecs {

  private[aws] val xmlToUrlFormHints = (hints: Hints) =>
    hints match {
      case Ec2QueryName.hint(ec2QueryName) => hints.addMemberHints(UrlFormName(ec2QueryName.value))
      case XmlName.hint(xmlName)           => hints.addMemberHints(UrlFormName(xmlName.value.capitalize))
      case _                               => hints
    }

  private[aws] val addErrorStartingPath = (_: Hints).add(XmlStartingPath(List("ErrorResponse", "Error")))

  def endpointPreprocessor(
      version: String
  ): PolyFunction5[Endpoint.Base, Endpoint.Base] =
    new PolyFunction5[Endpoint.Base, Endpoint.Base] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): Endpoint.Base[I, E, O, SI, SO] = {

        import AwsQueryCodecs.{addEndpointInfo, addDiscriminator}

        val inputTransformation = Schema.transformHintsLocallyK {
          xmlToUrlFormHints.andThen(addEndpointInfo(endpoint.id.name, version))
        }

        def errorTransformation = Covariant.liftPolyFunction[Option] {
          Errorable.transformErrorHintsLocallyK {
            addDiscriminator.andThen(addErrorStartingPath)
          }
        }

        val outputTransformation = Schema.transformHintsLocallyK {
          val responseTag = endpoint.name + "Response"
          (_: Hints).add(XmlStartingPath(List(responseTag)))
        }

        endpoint.builder
          .mapInput(inputTransformation(_))
          .mapOutput(outputTransformation(_))
          .mapErrorable(errorTransformation(_))
          .build
      }
    }

  // These are set to fulfil the requirements of
  // https://smithy.io/2.0/aws/protocols/aws-ec2-query-protocol.html?highlight=ec2%20query%20protocol#query-key-resolution.
  // without UrlFormDataEncoderSchemaVisitor having to be more aware
  // than necessary of these protocol quirks.
  private[aws] val inputEncoders = {
    UrlForm
      .Encoder(capitalizeStructAndUnionMemberNames = true)
      .mapK { UrlForm.Encoder.toWriterK.andThen(Writer.addingTo[Any].andThenK(form => Blob(form.render))) }
  }

  def make[F[_]: MonadThrowLike](
      version: String
  ): HttpUnaryClientCodecs.Builder[F, HttpRequest[Blob], HttpResponse[Blob]] = {

    HttpUnaryClientCodecs.builder
      .withEndpointPreprocessor(endpointPreprocessor(version))
      .withBodyEncoders(inputEncoders)
      .withSuccessBodyDecoders(Xml.readers)
      .withErrorBodyDecoders(Xml.readers)
      .withMetadataEncoders(Metadata.AwsEncoder)
      .withMetadataDecoders(Metadata.AwsDecoder)
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(Xml.readers))
      .withWriteEmptyStructs(_ => true)
      .withRequestMediaType("application/x-www-form-urlencoded")
  }

}
