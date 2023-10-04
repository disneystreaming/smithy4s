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

import smithy4s.Blob
import smithy4s.capability.MonadThrowLike
import smithy4s.http._
import smithy4s.xml.Xml
import smithy4s.xml.internals.XmlStartingPath

// scalafmt: {maxColumn = 120}

private[aws] object AwsRestXmlCodecs {

  def make[F[_]: MonadThrowLike](): HttpUnaryClientCodecs.Builder[F, HttpRequest[Blob], HttpResponse[Blob]] = {

    val errorDecoders = Xml.decoders.contramapSchema(
      smithy4s.schema.Schema.transformHintsLocallyK(
        _.addMemberHints(XmlStartingPath(List("ErrorResponse", "Error")))
      )
    )

    HttpUnaryClientCodecs.builder
      .withBodyEncoders(Xml.encoders)
      .withSuccessBodyDecoders(Xml.decoders)
      .withErrorBodyDecoders(errorDecoders)
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(errorDecoders))
      .withMetadataDecoders(Metadata.AwsDecoder)
      .withMetadataEncoders(Metadata.AwsEncoder)
      .withRawStringsAndBlobsPayloads
      .withRequestMediaType("application/xml")
  }

}
