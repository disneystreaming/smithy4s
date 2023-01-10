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

import cats.effect.Async
import cats.effect.Deferred
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import scala.concurrent.duration.FiniteDuration

private[compliancetests] class CompatEffect[F[_]](implicit
    val _Async: Async[F]
) extends CompatUtils[F] {
  // CE3 Deferred is in a cats.effect.Deferred
  def deferred[A]: F[Deferred[F, A]] = Deferred[F, A]
  // CE3 timeout is on Async
  def timeout[A](f: F[A], delay: FiniteDuration): F[A] =
    _Async.timeout(f, delay)

  // utf8 encode/decode under fs2.text.utf8
  val utf8Encode: fs2.Pipe[F, String, Byte] = fs2.text.utf8.encode[F]
  val utf8Decode: fs2.Pipe[F, Byte, String] = fs2.text.utf8.decode[F]
}

object Compat {
  def host(hostname: String): Host = Host.fromString(hostname).get
  def port(portNumber: Int): Port = Port.fromInt(portNumber).get

}

object CompatEffect {
  implicit def ce[F[_]: Async]: CompatEffect[F] = new CompatEffect[F]
}
