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

import smithy4s.capability.MonadThrowLike
import smithy4s.aws.kernel.`X-Amzn-Errortype`
import smithy4s.http.HttpDiscriminator
import smithy4s.schema._
import smithy4s.http.HttpResponse
import smithy4s.http.CaseInsensitive

// scalafmt: { maxColumn: 120 }
object AwsErrorTypeDecoder {

  private val errorTypeHeader = CaseInsensitive(`X-Amzn-Errortype`)

  private[aws] def fromResponse[F[_], Body](
      decoderCompiler: CachedSchemaCompiler[HttpResponse.Decoder[F, Body, *]]
  )(implicit F: MonadThrowLike[F]): HttpResponse[Body] => F[HttpDiscriminator] = {
    val decoder = decoderCompiler.fromSchema(AwsErrorType.schema)
    (response: HttpResponse[Body]) =>
      val maybeTypeHeader: Option[String] =
        response.headers
          .get(errorTypeHeader)
          .flatMap(_.headOption)
      val errorTypeF = maybeTypeHeader match {
        case Some(typeHeader) =>
          F.pure(
            AwsErrorType(
              __type = None,
              code = None,
              typeHeader = Some(typeHeader)
            )
          )
        case None =>
          decoder.read(response)
      }
      F.map(errorTypeF)(_.discriminator)
  }

  // See https://awslabs.github.io/smithy/1.0/spec/aws/aws-json-1_0-protocol.html#operation-error-serialization
  private[aws] case class AwsErrorType(
      __type: Option[String],
      code: Option[String],
      typeHeader: Option[String]
  ) {
    def discriminator: HttpDiscriminator = {
      __type
        .orElse(code)
        .orElse(typeHeader)
        .map { s =>
          val columnIndex = s.indexOf(':')
          val withoutColumn =
            if (columnIndex >= 0) s.substring(0, columnIndex) else s
          val hashIndex = withoutColumn.indexOf('#')
          if (hashIndex >= 0) withoutColumn.substring(hashIndex + 1)
          else withoutColumn
        }
        .map(HttpDiscriminator.NameOnly(_))
        .getOrElse(HttpDiscriminator.Undetermined)
    }
  }

  private[aws] object AwsErrorType {

    protected[aws] val schema: smithy4s.Schema[AwsErrorType] = {
      import smithy4s.schema.Schema._

      val __typeField = string
        .optional[AwsErrorType]("__type", _.__type)
      val codeField = string
        .optional[AwsErrorType]("code", _.code)
        .addHints(smithy.api.XmlName("Code"))
      val typeHeader =
        string
          .optional[AwsErrorType]("typeHeader", _.typeHeader)
          .addHints(
            smithy.api.HttpHeader(`X-Amzn-Errortype`)
          )
      struct(__typeField, codeField, typeHeader)(AwsErrorType.apply)
    }
  }

}
