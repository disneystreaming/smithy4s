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
import smithy4s.codecs.Writer
import smithy4s._
import smithy4s.http._
import smithy4s.xml.Xml
import smithy4s.capability.{Covariant, MonadThrowLike}
import smithy4s.http.internals.StaticUrlFormElements
import smithy4s.xml.internals.XmlStartingPath
import smithy4s.kinds.PolyFunction5

// scalafmt: { maxColumn = 120}
private[aws] object AwsQueryCodecs {

  // The AWS protocol works in terms of XmlFlattened and XmlName hints,
  // even for the input side, which is most definitely _not_ XML. Partly
  // because that seems odd, but mostly so that the URL form support can
  // be completely agnostic of AWS protocol details, they work with their
  // own more appropriately named hints - which is what necessitates the
  // translation here.
  val transformXmlHints = Schema.transformHintsTransitivelyK { hints =>
    def translateFlattened(hints: Hints): Hints =
      hints.memberHints.get(XmlFlattened) match {
        case Some(_) => hints.addMemberHints(UrlFormFlattened())
        case None    => hints
      }
    def translateName(hints: Hints): Hints =
      hints.memberHints.get(XmlName) match {
        case Some(XmlName(name)) =>
          hints.addMemberHints(UrlFormName(name))
        case None => hints
      }
    (translateFlattened _ andThen translateName _)(hints)
  }

  val inputEncoders = {
    UrlForm
      .Encoder(ignoreUrlFormFlattened = false, capitalizeStructAndUnionMemberNames = false)
      .mapK { UrlForm.Encoder.toWriterK.andThen(Writer.addingTo[Any].andThenK(form => Blob(form.render))) }
  }

  def endpointPreprocessor(version: String): PolyFunction5[Endpoint.Base, Endpoint.Base] =
    new PolyFunction5[Endpoint.Base, Endpoint.Base] {
      def apply[I, E, O, SI, SO](endpoint: Endpoint.Base[I, E, O, SI, SO]): Endpoint.Base[I, E, O, SI, SO] = {

        val inputTransformation = {
          val staticUrlFormData = StaticUrlFormElements(List(("Action" -> endpoint.id.name), ("Version" -> version)))
          val addStaticUrlFormData =
            Schema.transformHintsLocallyK(_.addMemberHints(staticUrlFormData))
          transformXmlHints.andThen(addStaticUrlFormData)
        }

        def errorTransformation = {
          // Takes the `@awsQueryError` trait into consideration to decide how to
          // discriminate error responses.
          val addErrorName = (hints: Hints) =>
            hints.get(AwsQueryError) match {
              case Some(awsQueryError) =>
                val newHint = smithy4s.http.internals.ErrorDiscriminatorValue(awsQueryError.code)
                hints.addMemberHints(newHint)
              case None => hints
            }
          // The actual payloads are nested under two layers of XML
          val nestedXml = (_: Hints).addMemberHints(XmlStartingPath(List("ErrorResponse", "Error")))
          Covariant.liftPolyFunction[Option](Errorable.transformErrorHintsLocallyK(addErrorName.andThen(nestedXml)))
        }

        val outputTransformation = {
          val responseTag = endpoint.name + "Response"
          val resultTag = endpoint.name + "Result"
          Schema.transformHintsLocallyK(_.addMemberHints(XmlStartingPath(List(responseTag, resultTag))))
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
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(Xml.readers))
      .withWriteEmptyStructs(_ => true)
      .withRequestMediaType("application/x-www-form-urlencoded")

  }

}
