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

sealed trait GrpcCompression extends Product with Serializable {
  def name: String
  def isEnabled: Boolean
}

object GrpcCompression {
  case object Identity extends GrpcCompression {
    val name: String = "identity"
    val isEnabled: Boolean = false
  }

  case object Gzip extends GrpcCompression {
    val name: String = "gzip"
    val isEnabled: Boolean = true
  }

  case object Deflate extends GrpcCompression {
    val name: String = "deflate"
    val isEnabled: Boolean = true
  }

  final case class Custom(name: String) extends GrpcCompression {
    val isEnabled: Boolean = true
  }

  final case class UnknownCompression(value: String)

  val supported: List[GrpcCompression] = List(Identity, Gzip, Deflate)

  def fromString(value: String): Either[UnknownCompression, GrpcCompression] =
    // gRPC compression names are case-insensitive; we normalize unknowns to lowercase.
    value.toLowerCase match {
      case "identity" => Right(Identity)
      case "gzip"     => Right(Gzip)
      case "deflate"  => Right(Deflate)
      case other       => Left(UnknownCompression(other))
    }

  def parseLenient(value: String): GrpcCompression =
    fromString(value).getOrElse(Custom(value))
}
