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

package smithy4s.compliancetests

import cats.effect.Deferred
import cats.effect.IO
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

private[compliancetests] class CompatEffect {
  def deferred[A]: IO[Deferred[IO, A]] = Deferred[IO, A]

  val utf8Encode: fs2.Pipe[IO, String, Byte] = fs2.text.utf8.encode[IO]
}

object Compat {
  def host(hostname: String): Host = Host.fromString(hostname).get
  def port(portNumber: Int): Port = Port.fromInt(portNumber).get

}

object CompatEffect {
  implicit val ce: CompatEffect = new CompatEffect
}
