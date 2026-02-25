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

package smithy4s.grpc

final case class GrpcMethodPath(service: String, method: String) {
  def render: String = s"/$service/$method"
}

object GrpcMethodPath {
  sealed trait ParseError extends Product with Serializable
  case object InvalidFormat extends ParseError

  def parse(rawPath: String): Either[ParseError, GrpcMethodPath] = {
    val normalized =
      if (rawPath.length > 1 && rawPath.endsWith("/")) rawPath.dropRight(1) else rawPath
    val segments = normalized.split('/').filter(_.nonEmpty).toList
    segments match {
      case service :: method :: Nil => Right(GrpcMethodPath(service, method))
      case _                        => Left(InvalidFormat)
    }
  }

  def parseSegments(segments: List[String], hasTrailingSlash: Boolean): Either[ParseError, GrpcMethodPath] =
    segments match {
      case service :: method :: Nil => Right(GrpcMethodPath(service, method))
      case Nil if hasTrailingSlash  => Left(InvalidFormat)
      case _                        => Left(InvalidFormat)
    }
}
