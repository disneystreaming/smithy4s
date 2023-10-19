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

package smithy4s.dynamic
import smithy4s.tests._
import scala.concurrent.Future

object DummyIO {
  type IO[A] = Either[Throwable, A]
  object IO {
    def apply[A](a: => A) = try { Right(a) }
    catch { case e: Throwable => Left(e) }
    def pure[A](a: A): IO[A] = Right(a)
    def raiseError[A](e: Throwable): IO[A] = Left(e)
  }

  trait Suite extends munit.FunSuite {
    override def munitValueTransforms: List[ValueTransform] =
      ioValueTransform :: super.munitValueTransforms

    private val ioValueTransform: ValueTransform =
      new ValueTransform(
        "DummyIO",
        {
          case Left(ex: Throwable) => Future.failed(ex)
          case Right(v)            => Future.successful(v)
        }
      )
  }

  object JsonIOProtocol extends JsonProtocolF[IO]
}
