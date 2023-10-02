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
import org.http4s.Header
import org.http4s.Request
import org.http4s.headers.`Content-Encoding`
import org.http4s.headers.`Content-Length`

// inspired from:
// https://github.com/http4s/http4s/blob/v0.23.19/client/shared/src/main/scala/org/http4s/client/middleware/GZip.scala
private[smithy4s] object GzipRequestCompression {
  val DefaultBufferSize = 32 * 1024

  def apply[F[_]: Compression](
      // This is used by AwsQueryCodecs and AwsEc2QueryCodecs to conform to the
      // requirements of
      // https://github.com/smithy-lang/smithy/blob/main/smithy-aws-protocol-tests/model/awsQuery/requestCompression.smithy#L152-L298
      // and
      // https://github.com/smithy-lang/smithy/blob/main/smithy-aws-protocol-tests/model/ec2Query/requestCompression.smithy#L152-L298.
      retainUserEncoding: Boolean,
      bufferSize: Int = DefaultBufferSize,
      level: DeflateParams.Level = DeflateParams.Level.DEFAULT
  ): Request[F] => Request[F] = { request =>
    val updateContentTypeEncoding =
      (retainUserEncoding, request.headers.get[`Content-Encoding`]) match {
        case (true, Some(`Content-Encoding`(cc))) =>
          Header.Raw(
            `Content-Encoding`.headerInstance.name,
            s"${cc.coding}, gzip"
          )

        case _ =>
          Header.Raw(
            `Content-Encoding`.headerInstance.name,
            "gzip"
          )
      }
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
    request
      .removeHeader[`Content-Length`]
      .putHeaders(updateContentTypeEncoding)
      .withBodyStream(request.body.through(compressPipe))
  }

}
