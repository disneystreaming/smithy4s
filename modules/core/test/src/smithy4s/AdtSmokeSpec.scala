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

import munit._
import smithy4s.example._

class AdtSmokeSpec() extends FunSuite {

  test("Idiomatic ADTs can be generated from unions") {
    // Checking that these things compile okay and that their document encoding is correct
    val x1: OrderType = OrderType.InStoreOrder(OrderNumber(1), None)
    val x2: OrderType = OrderType.PreviewCase
    val x3: OrderType = OrderType.OnlineCase(OrderNumber(1))

    val d1 = Document.encode(x1)
    val d2 = Document.encode(x2)
    val d3 = Document.encode(x3)
    val expected1 =
      Document.obj("inStore" -> Document.obj("id" -> Document.fromInt(1)))
    val expected2 = Document.obj("preview" -> Document.obj())
    val expected3 = Document.obj("online" -> Document.fromInt(1))
    assertEquals(d1, expected1)
    assertEquals(d2, expected2)
    assertEquals(d3, expected3)
  }

}
