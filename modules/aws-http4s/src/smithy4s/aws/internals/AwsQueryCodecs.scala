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
import alloy.UrlFormFlattened
import alloy.UrlFormName
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s._
import smithy4s.capability.Covariant
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.Writer
import smithy4s.http._
import smithy4s.http.internals.StaticUrlFormElements
import smithy4s.kinds.PolyFunction5
import smithy4s.xml.Xml
import smithy4s.xml.internals.XmlStartingPath

// scalafmt: { maxColumn = 120}
private[aws] object AwsQueryCodecs {

  // The AWS protocol works in terms of XmlFlattened and XmlName hints,
  // even for the input side, which is most definitely _not_ XML. Partly
  // because that seems odd, but mostly so that the URL form support can
  // be completely agnostic of AWS protocol details, they work with their
  // own more appropriately named hints - which is what necessitates the
  // translation here.
  private[aws] val xmlToUrlFormHints = (hints: Hints) =>
    hints
      .expand((_: XmlFlattened) => UrlFormFlattened())
      .expand((xmlName: XmlName) => UrlFormName(xmlName.value))

  private[aws] val inputEncoders = {
    UrlForm
      .Encoder(capitalizeStructAndUnionMemberNames = false)
      .mapK { UrlForm.Encoder.toWriterK.andThen(Writer.addingTo[Any].andThenK(form => Blob(form.render))) }
  }

  // Takes the `@awsQueryError` trait into consideration to decide how to
  // discriminate error responses.
  private[aws] val addDiscriminator = (_: Hints).expand { (awsQueryError: AwsQueryError) =>
    smithy4s.http.internals.ErrorDiscriminatorValue(awsQueryError.code)
  }

  // The actual error payloads are nested under two layers of XML
  private[aws] val addErrorStartingPath = (_: Hints).add(XmlStartingPath(List("ErrorResponse", "Error")))

  // The name of the endpoint and version of the service must be added to the body
  private[aws] def addEndpointInfo(endpointName: String, version: String) = (hints: Hints) =>
    hints.add(StaticUrlFormElements(List(("Action" -> endpointName), ("Version" -> version))))

  private[aws] val discriminatorReaders =
    Xml.readers.contramapSchema(Schema.transformHintsLocallyK(addErrorStartingPath))

  private def endpointPreprocessor(version: String): PolyFunction5[Endpoint.Base, Endpoint.Base] =
    new PolyFunction5[Endpoint.Base, Endpoint.Base] {
      def apply[I, E, O, SI, SO](endpoint: Endpoint.Base[I, E, O, SI, SO]): Endpoint.Base[I, E, O, SI, SO] = {

        val inputTransformation = Schema.transformHintsTransitivelyK {
          xmlToUrlFormHints.andThen(addEndpointInfo(endpoint.id.name, version))
        }

        def errorTransformation = Covariant.liftPolyFunction[Option] {
          val transitive = Errorable.transformHintsTransitivelyK(xmlToUrlFormHints)
          val local = Errorable.transformHintsLocallyK {
            xmlToUrlFormHints.andThen(addDiscriminator).andThen(addErrorStartingPath)
          }
          local.andThen(transitive)
        }

        val outputTransformation = Schema.transformHintsLocallyK {
          val responseTag = endpoint.name + "Response"
          val resultTag = endpoint.name + "Result"
          (_: Hints).add(XmlStartingPath(List(responseTag, resultTag)))
        }

        endpoint.builder
          .mapInput(inputTransformation(_))
          .mapOutput(outputTransformation(_))
          .mapErrorable(errorTransformation(_))
          .build
      }
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
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(discriminatorReaders))
      .withWriteEmptyStructs(_ => true)
      .withRequestMediaType("application/x-www-form-urlencoded")

  }

}
