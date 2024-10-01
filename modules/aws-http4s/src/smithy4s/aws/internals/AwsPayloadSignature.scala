/*
 *  Copyright 2021-2024 Disney Streaming
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

import cats.effect.Concurrent
import cats.effect.Resource
import cats.syntax.all._
import fs2.Chunk
import org.http4s._
import org.http4s.client.Client
import org.typelevel.ci.CIString
import smithy4s._
import smithy4s.aws.kernel.AwsCrypto._

private[aws] sealed trait AwsPayloadSignature {
  import AwsPayloadSignature._
  val headerValue: String = this match {
    case Sha256(v) => v
    case UnsignedPayload => "UNSIGNED-PAYLOAD"
    // case StreamingUnsignedPayload => "STREAMING-UNSIGNED-PAYLOAD-TRAILER"
  }
}

/**
 * This is a draft API. There are many other ways to include the payload in the signature.
 * Some of which are complex: using trailers and/or multiple chunks
 */
private[aws] object AwsPayloadSignature {
  case class Sha256(value: String) extends AwsPayloadSignature
  case object UnsignedPayload extends AwsPayloadSignature
  // case object StreamingUnsignedPayload extends AwsPayloadSignature

  val `X-Amz-Content-SHA256` = CIString("X-Amz-Content-SHA256")

  def makeHeader(value: AwsPayloadSignature): Header.Raw =
    Header.Raw(`X-Amz-Content-SHA256`, value.headerValue)


  def signSingleChunk[F[_]: Concurrent]: Endpoint.Middleware[Client[F]] =
    new Endpoint.Middleware[Client[F]] {
      def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: service.Endpoint[_, _, _, _, _]
      ): Client[F] => Client[F] = { client =>
        Client { request =>
          Resource.eval(hashSingleChunk(request)).flatMap { request =>
            client.run(request)
          }
        }
      }
    }

  private def hashSingleChunk[F[_]: Concurrent](
      request: Request[F]
  ): F[Request[F]] = {
    request.body.chunks.compile.to(Chunk).map(_.flatten).map { body =>
      val payloadHash = sha256HexDigest(body.toArray)
      val signature = AwsPayloadSignature.Sha256(payloadHash)
      request.putHeaders(AwsPayloadSignature.makeHeader(signature))
    }
  }
}
