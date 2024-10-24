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

package smithy4s

import munit._
import smithy4s.example._

final class VisitorAndProjectionSpec extends FunSuite {

  test("visitor") {
    val visitor = new OrderType.Visitor[String] {
      def online(value: OrderNumber): String = s"ONLINE ${value.value}"
      def inStore(value: OrderType.InStoreOrder): String = s"IN STORE $value"
      def preview(value: OrderType.PreviewCase.type): String = "PREVIEW"
    }
    assertEquals(
      OrderType.OnlineCase(OrderNumber(123)).accept(visitor),
      "ONLINE 123"
    )
    assertEquals(
      OrderType.InStoreOrder(OrderNumber(2)).accept(visitor),
      "IN STORE InStoreOrder(2,None)"
    )
    assertEquals(
      OrderType.PreviewCase.accept(visitor),
      "PREVIEW"
    )
  }

  test("visitor - default") {
    val visitor = new OrderType.Visitor.Default[String] {
      def default: String = "test"
    }
    assertEquals(
      OrderType.OnlineCase(OrderNumber(123)).accept(visitor),
      "test"
    )
    assertEquals(
      OrderType.InStoreOrder(OrderNumber(2)).accept(visitor),
      "test"
    )
    assertEquals(
      OrderType.PreviewCase.accept(visitor),
      "test"
    )
  }

  test("projections") {
    val one = OrderType.OnlineCase(OrderNumber(123))
    val two = OrderType.InStoreOrder(OrderNumber(2))
    val three = OrderType.PreviewCase

    assertEquals(
      one.widen.project.online,
      Option(one.online)
    )
    assertEquals(
      two.widen.project.inStore,
      Option(two)
    )
    assertEquals(
      three.widen.project.preview,
      Option(three)
    )

    assertEquals(
      two.widen.project.online,
      None
    )
    assertEquals(
      three.widen.project.online,
      None
    )
  }

}
