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

package smithy4s.dynamic
import smithy4s.tests._

object DummyIO {
  type IO[A] = Either[Throwable, A]
  object IO {
    def apply[A](a: => A) = try { Right(a) }
    catch { case e: Throwable => Left(e) }
    def pure[A](a: A): IO[A] = Right(a)
    def raiseError[A](e: Throwable): IO[A] = Left(e)
  }
  implicit class IOOps[A](private val io: IO[A]) extends AnyVal {
    def mapRun[B](f: A => B): B = io match {
      case Left(e)  => throw e
      case Right(a) => f(a)
    }
    def check(): Unit = io match {
      case Left(e)  => throw e
      case Right(_) => ()
    }
  }
  object JsonIOProtocol extends JsonProtocolF[IO]
}
