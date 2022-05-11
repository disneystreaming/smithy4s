package smithy4s.example

import smithy4s.schema.Schema._

case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None)
object InStoreOrder extends smithy4s.ShapeTag.Companion[InStoreOrder] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "InStoreOrder")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[InStoreOrder] = struct(
    OrderNumber.schema.required[InStoreOrder]("id", _.id).addHints(smithy.api.Required()),
    string.optional[InStoreOrder]("locationId", _.locationId),
  ){
    InStoreOrder.apply
  }.withId(id).addHints(hints)
}