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
