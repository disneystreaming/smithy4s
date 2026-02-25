/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.grpc.internals

import smithy4s.Blob

import java.util.Base64
import scala.util.Try

private[grpc] object GrpcMetadataCodec {
  def decodeBinaryHeaderValue(value: String): Either[Throwable, Vector[Blob]] = {
    // gRPC binary headers may be comma-joined; empty entries are valid and decode to empty bytes.
    // gRPC requires accepting both padded and unpadded standard base64 encodings.
    // Base64 decoder accepts padded and unpadded input per JDK 8+ contract.
    val decoder = Base64.getDecoder
    val tokens = value.split(",", -1).toVector.map(_.trim)
    tokens.foldLeft(Right(Vector.empty): Either[Throwable, Vector[Blob]]) {
      case (Right(acc), token) =>
        Try(decoder.decode(token)).toEither.map(bytes => acc :+ Blob(bytes))
      case (left @ Left(_), _) => left
    }
  }

  def encodeBinaryHeaderValue(blob: Blob): String =
    Base64.getEncoder.withoutPadding().encodeToString(blob.toArray)

}
