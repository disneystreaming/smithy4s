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

import cats.effect._
import cats.effect.concurrent.Deferred

import scala.concurrent.duration.FiniteDuration

private[compliancetests] class CompatEffect[F[_]](implicit
    val _Concurrent: Concurrent[F],
    val _Timer: Timer[F]
) extends CompatUtils[F] {
  // CE2 Deferred is in a cats.effect.concurrent.Deferred
  def deferred[A]: F[Deferred[F, A]] = Deferred[F, A]
  // CE2 timeout is on Concurrent
  def timeout[A](f: F[A], delay: FiniteDuration): F[A] =
    Concurrent.timeout[F, A](f, delay)

  // utf8 encode/decode under fs2.text
  val utf8Encode: fs2.Pipe[F, String, Byte] = fs2.text.utf8Encode[F]
  val utf8Decode: fs2.Pipe[F, Byte, String] = fs2.text.utf8Decode[F]
}

object Compat {
  def host(hostname: String): String = hostname
  def port(portNumber: Int): Int = portNumber
}

object CompatEffect {
  implicit def ce[F[_]](implicit
      cs: Concurrent[F],
      timer: Timer[F]
  ): CompatEffect[F] = new CompatEffect[F]
}
