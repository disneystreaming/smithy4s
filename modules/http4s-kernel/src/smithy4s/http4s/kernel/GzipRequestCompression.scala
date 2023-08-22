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

package smithy4s.http4s.kernel

import fs2.compression.Compression
import fs2.compression.DeflateParams
import fs2.compression.ZLibParams
import smithy4s.http.HttpRequest
import smithy4s.http.CaseInsensitive
import org.http4s.Entity

// inspired from:
// https://github.com/http4s/http4s/blob/v0.23.19/client/shared/src/main/scala/org/http4s/client/middleware/GZip.scala
object GzipRequestCompression {
  val DefaultBufferSize = 32 * 1024

  private val CONTENT_LENGTH = CaseInsensitive("Content-Length")
  private val CONTENT_ENCODING = CaseInsensitive("Content-Encoding")

  def apply[F[_]: Compression](
      // This is used by AwsQueryCodecs and AwsEc2QueryCodecs to conform to the
      // requirements of
      // https://github.com/smithy-lang/smithy/blob/main/smithy-aws-protocol-tests/model/awsQuery/requestCompression.smithy#L152-L298
      // and
      // https://github.com/smithy-lang/smithy/blob/main/smithy-aws-protocol-tests/model/ec2Query/requestCompression.smithy#L152-L298.
      retainUserEncoding: Boolean,
      bufferSize: Int = DefaultBufferSize,
      level: DeflateParams.Level = DeflateParams.Level.DEFAULT
  ): HttpRequest[Entity[F]] => HttpRequest[Entity[F]] = {
    val compressPipe =
      Compression[F].gzip(
        fileName = None,
        modificationTime = None,
        comment = None,
        DeflateParams(
          bufferSize = bufferSize,
          level = level,
          header = ZLibParams.Header.GZIP
        )
      )

    request =>
      val contentEncoding = request.headers
        .get(CONTENT_ENCODING)
        .flatMap(_.headOption)
      val updatedHeaders =
        (retainUserEncoding, contentEncoding) match {
          case (true, Some(cc)) =>
            request.headers - CONTENT_LENGTH + (CONTENT_ENCODING -> Seq(
              s"${cc}, gzip"
            ))
          case _ =>
            request.headers - CONTENT_LENGTH + (CONTENT_ENCODING -> Seq("gzip"))
        }

      val updatedBody =
        request.body.copy(body = compressPipe(request.body.body))
      request
        .copy(headers = updatedHeaders, body = updatedBody)
  }

}
