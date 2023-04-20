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
package schema

import Schema._
import munit._

final class AltSpec extends FunSuite {

  test("dispatcher projector") {
    type Foo = Either[Int, String]
    val left = int.oneOf[Foo]("left", Left(_))
    val right = string.oneOf[Foo]("right", Right(_))
    val schema = union(left, right) {
      case Left(int)     => left(int)
      case Right(string) => right(string)
    }

    val dispatcher = Alt.Dispatcher.fromUnion(schema)

    val projectedLeft = dispatcher.projector[Int](left)

    val projectedRight = dispatcher.projector[String](right)

    assertEquals(projectedLeft(Left(100)), Option(100))
    assertEquals(projectedLeft(Right("100")), None)
    assertEquals(projectedRight(Right("100")), Option("100"))
    assertEquals(projectedRight(Left(100)), None)
  }

}
