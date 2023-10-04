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

import cats.effect.MonadCancelThrow
import fs2.Pipe
import fs2.Pull
import fs2.Stream
import fs2.compression.Compression
import org.http4s.ContentCoding
import org.http4s.Header
import org.http4s.Request
import org.http4s.headers.`Content-Encoding`
import org.http4s.headers.`Content-Length`

import scala.util.control.NoStackTrace

// inspired from:
// https://github.com/http4s/http4s/blob/v0.23.19/server/shared/src/main/scala/org/http4s/server/middleware/GZip.scala
private[smithy4s] object GzipRequestDecompression {
  val DefaultBufferSize = 32 * 1024

  def apply[F[_]: MonadCancelThrow: Compression](
      bufferSize: Int = DefaultBufferSize
  ): Request[F] => Request[F] = {
    def decompressWith(bufferSize: Int): Pipe[F, Byte, Byte] =
      _.pull.peek1
        .flatMap {
          case None                  => Pull.raiseError(EmptyBodyException)
          case Some((_, fullStream)) => Pull.output1(fullStream)
        }
        .stream
        .flatten
        .through(Compression[F].gunzip(bufferSize))
        .flatMap(_.content)
        .handleErrorWith {
          case EmptyBodyException => Stream.empty
          case error              => Stream.raiseError(error)
        }

    (request: Request[F]) =>
      request.headers.get[`Content-Encoding`] match {
        case Some(`Content-Encoding`(ContentCoding.gzip)) =>
          val updatedRequest =
            request
              .filterHeaders(nonCompressionHeader)
              .withBodyStream(
                request.body.through(decompressWith(bufferSize))
              )
          updatedRequest
        case Some(_) | None =>
          request
      }
  }

  private def nonCompressionHeader(header: Header.Raw): Boolean =
    header.name != `Content-Length`.headerInstance.name &&
      header.name != `Content-Encoding`.headerInstance.name

  private object EmptyBodyException extends Throwable with NoStackTrace
}
