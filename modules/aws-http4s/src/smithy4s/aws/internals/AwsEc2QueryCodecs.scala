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
import alloy.UrlFormFlattened
import smithy.api.XmlName
import smithy4s._
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.Writer
import smithy4s.http._
import smithy4s.kinds.PolyFunction5
import smithy4s.xml.Xml
import smithy4s.xml.internals.XmlStartingPath
import smithy4s.schema.OperationSchema
import smithy4s.schema.ErrorSchema

// scalafmt: { maxColumn = 120}
private[aws] object AwsEcsQueryCodecs {

  // These are set to fulfil the requirements of
  // https://smithy.io/2.0/aws/protocols/aws-ec2-query-protocol.html?highlight=ec2%20query%20protocol#query-key-resolution.
  private val xmlToUrlFormHints = (hints: Hints) =>
    hints match {
      case Ec2QueryName.hint(ec2QueryName) => hints.addMemberHints(UrlFormName(ec2QueryName.value))
      case XmlName.hint(xmlName)           => hints.addMemberHints(UrlFormName(xmlName.value.capitalize))
      case _                               => hints
    }

  // All collections are encoded supposed to be using the flattened encoding. We simply modify the schema transitively
  // to add the hint to all layers, for simplicity.
  private val flattenAll = (_: Hints).add(UrlFormFlattened())

  private val addErrorStartingPath = (_: Hints).add(XmlStartingPath(List("Response", "Errors", "Error")))
  private val discriminatorReaders =
    Xml.decoders.contramapSchema(Schema.transformHintsLocallyK(addErrorStartingPath))

  def operationPreprocessor(
      version: String
  ): PolyFunction5[OperationSchema, OperationSchema] =
    new PolyFunction5[OperationSchema, OperationSchema] {
      def apply[I, E, O, SI, SO](
          operation: OperationSchema[I, E, O, SI, SO]
      ): OperationSchema[I, E, O, SI, SO] = {

        import AwsQueryCodecs.{addOperationInfo, addDiscriminator}

        val inputTransformation = {
          val transitive = Schema.transformHintsTransitivelyK { xmlToUrlFormHints.andThen(flattenAll) }
          val local = Schema.transformHintsLocallyK(addOperationInfo(operation.id.name, version))
          transitive.andThen(local)
        }

        def errorTransformation =
          ErrorSchema.transformHintsLocallyK {
            addDiscriminator.andThen(addErrorStartingPath)
          }

        val outputTransformation = Schema.transformHintsLocallyK {
          val responseTag = operation.id.name + "Response"
          (_: Hints).add(XmlStartingPath(List(responseTag)))
        }

        operation
          .mapInput(inputTransformation(_))
          .mapOutput(outputTransformation(_))
          .mapError(errorTransformation(_))
      }
    }

  // These are set to fulfil the requirements of
  // https://smithy.io/2.0/aws/protocols/aws-ec2-query-protocol.html?highlight=ec2%20query%20protocol#query-key-resolution.
  // without UrlFormDataEncoderSchemaVisitor having to be more aware than necessary of these protocol quirks.
  private[aws] val inputEncoders = {
    UrlForm
      .Encoder(capitalizeStructAndUnionMemberNames = true)
      .mapK { UrlForm.Encoder.toWriterK.andThen(Writer.addingTo[Any].andThenK(form => Blob(form.render))) }
  }

  def make[F[_]: MonadThrowLike](
      version: String
  ): HttpUnaryClientCodecs.Builder[F, HttpRequest[Blob], HttpResponse[Blob]] = {

    HttpUnaryClientCodecs.builder
      .withOperationPreprocessor(operationPreprocessor(version))
      .withBodyEncoders(inputEncoders)
      .withSuccessBodyDecoders(Xml.decoders)
      .withErrorBodyDecoders(Xml.decoders)
      .withMetadataEncoders(Metadata.AwsEncoder)
      .withMetadataDecoders(Metadata.AwsDecoder)
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(discriminatorReaders))
      .withWriteEmptyStructs(_ => true)
      .withRequestMediaType("application/x-www-form-urlencoded")
  }

}
