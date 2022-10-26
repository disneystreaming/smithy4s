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

package smithy4s

import smithy4s.example._

import munit._
import scala.util.Try
class TransformationSmokeSpec() extends FunSuite {

  test("transform method can be called with poly functions") {
    object stub extends Weather[Option] {
      def getCurrentTime(): Option[GetCurrentTimeOutput] = None
    }

    case object Empty extends Throwable

    // Not ascribing the type to get a
    val transformed = stub.transform(new PolyFunction[Option, Try] {
      def apply[A](fa: Option[A]): Try[A] = fa match {
        case Some(value) => scala.util.Success(value)
        case None        => scala.util.Failure(Empty)
      }
    })
    expect(transformed.getCurrentTime().isFailure)
  }

}
