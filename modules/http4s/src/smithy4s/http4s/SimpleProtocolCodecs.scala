/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.http4s

import org.http4s.{Uri, Request, Response}
import cats.effect.Concurrent
import smithy4s.client._
import smithy4s.server.UnaryServerCodecs

// scalafmt: { maxColumn = 120 }
trait SimpleProtocolCodecs {

  def makeServerCodecs[F[_]: Concurrent]: UnaryServerCodecs.Make[F, Request[F], Response[F]]
  def makeClientCodecs[F[_]: Concurrent](baseUri: Uri): UnaryClientCodecs.Make[F, Request[F], Response[F]]

}
