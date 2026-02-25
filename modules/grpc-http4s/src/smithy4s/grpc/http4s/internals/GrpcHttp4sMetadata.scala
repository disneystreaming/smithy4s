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

package smithy4s.grpc.http4s.internals

import org.http4s.{Header, Headers}
import org.typelevel.ci.CIString
import smithy4s.capability.MonadThrowLike
import smithy4s.grpc.GrpcMetadata
import smithy4s.grpc.internals.GrpcMetadataCodec

private[http4s] object GrpcHttp4sMetadata {
  def fromApplicationHeaders[F[_]](
      headers: Headers
  )(implicit F: MonadThrowLike[F]): F[GrpcMetadata] =
    decodeHeaders(headers) { name =>
      val lower = name.toLowerCase
      !lower.startsWith(":") &&
      !lower.startsWith("grpc-") &&
      lower != "content-type" &&
      lower != "te"
    }

  def fromTrailers[F[_]](
      headers: Headers
  )(implicit F: MonadThrowLike[F]): F[GrpcMetadata] =
    decodeHeaders(headers)(name => !name.startsWith(":"))

  private def decodeHeaders[F[_]](
      headers: Headers
  )(include: String => Boolean)(implicit F: MonadThrowLike[F]): F[GrpcMetadata] =
    headers.headers.foldLeft(F.pure(GrpcMetadata.empty)) { (accF, header) =>
      val name = header.name.toString
      if (!include(name)) accF
      else {
        F.flatMap(accF) { acc =>
          if (name.toLowerCase.endsWith("-bin")) {
            F.flatMap(F.liftEither(GrpcMetadataCodec.decodeBinaryHeaderValue(header.value))) {
              blobs =>
                F.pure(blobs.foldLeft(acc) { (innerAcc, blob) =>
                  innerAcc.addBinary(name, blob)
                })
            }
          } else {
            F.pure(acc.addText(name, header.value))
          }
        }
      }
    }

  def toHeaders(metadata: GrpcMetadata): Headers = {
    metadata.values.foldLeft(Headers.empty) { case (acc, (key, values)) =>
      val name = key.toString
      values.foldLeft(acc) { (headers, value) =>
        val rawValue = value match {
          case GrpcMetadata.Value.Text(text) => text
          case GrpcMetadata.Value.Binary(blob) =>
            GrpcMetadataCodec.encodeBinaryHeaderValue(blob)
        }
        headers ++ Headers(Header.Raw(CIString(name), rawValue))
      }
    }
  }
}
