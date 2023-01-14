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

package smithy4s.aws.query

import cats.effect.SyncIO
import cats.syntax.either._
import smithy4s.http.{BodyPartial, CodecAPI, HttpMediaType, PayloadError}
import smithy4s.schema.CompilationCache
import smithy4s.PayloadPath
import smithy4s.xml.XmlDocument
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.dom._

import java.nio.ByteBuffer
import smithy4s.schema.Schema
import smithy.api.XmlName
import smithy4s.internals.InputOutput

private[aws] class AwsQueryCodecAPI(
    operationName: String,
    serviceVersion: String
) extends CodecAPI {

  override type Codec[A] = Either[AwsQueryCodec[A], XmlDocument.Decoder[A]]
  override type Cache = CompilationCache[AwsQueryCodec]

  override def createCache(): Cache = CompilationCache.make[AwsQueryCodec]

  override def compileCodec[A](
      schema: Schema[A],
      cache: Cache
  ): Codec[A] =
    schema.hints.get(InputOutput) match {
      case Some(InputOutput.Input) =>
        val visitor = new AwsSchemaVisitorAwsQueryCodec(cache)
        val awsQueryEncoder = schema.compile(visitor)
        Left(awsQueryEncoder)
      case Some(InputOutput.Output) | None =>
        val responseSchema =
          AwsQueryCodecAPI.xmlResponseSchema(operationName, schema)
        val xmlDecoder = XmlDocument.Decoder.fromSchema(responseSchema)
        Right(xmlDecoder)
    }

  override def mediaType[A](codec: Codec[A]): HttpMediaType =
    HttpMediaType("application/x-www-form-urlencoded")

  override def decodeFromByteArrayPartial[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, BodyPartial[A]] = codec match {
    case Left(_) =>
      Left(
        PayloadError(
          PayloadPath.root,
          "",
          "Invalid codec: got AWS Query encoder, expected XML decoder"
        )
      )
    case Right(xmlDecoder) =>
      Stream
        .emit[SyncIO, String](new String(bytes, "UTF-8"))
        .through(events[SyncIO, String]())
        .through(documents[SyncIO, XmlDocument])
        .map(xmlDecoder.decode)
        .rethrow
        .head
        .compile
        .lastOrError
        .map(a => BodyPartial(_ => a))
        .attempt
        .unsafeRunSync()
        .leftMap(e =>
          PayloadError(
            PayloadPath.root,
            "",
            s"Failed to decode the response, message: ${e.getMessage}"
          )
        )
  }

  override def decodeFromByteBufferPartial[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, BodyPartial[A]] =
    throw new IllegalStateException("Must have not been called")

  override def writeToArray[A](codec: Codec[A], value: A): Array[Byte] =
    codec match {

      case Left(encoder) =>
        val formData = encoder(value)
        val operationNameValue =
          FormData.PathedValue(PayloadPath("Action"), operationName)
        val versionValue =
          FormData.PathedValue(PayloadPath("Version"), serviceVersion)
        FormData
          .MultipleValues(Vector(formData, operationNameValue, versionValue))
          .render
          .getBytes("UTF-8")

      case Right(_) =>
        throw new IllegalStateException(
          "Invalid codec: got XML decoder, must be AWS query encoder"
        )
    }
}

private[aws] object AwsQueryCodecAPI {

  /**
    * Amend the schema to be able to work the the response xml payload, which wraps the output
    * in two different layers of xml nodes.
    *
    * See https://smithy.io/2.0/aws/protocols/aws-query-protocol.html?highlight=aws%20query%20protocol#response-serialization
    */
  private def xmlResponseSchema[A](
      operationName: String,
      outputSchema: Schema[A]
  ): Schema[A] = {
    val resultName = operationName + "Result"
    val responseName = operationName + "Response"
    val resultField =
      outputSchema.required[A](resultName, identity[A])
    Schema.struct(resultField)(identity[A]).addHints(XmlName(responseName))
  }

}
