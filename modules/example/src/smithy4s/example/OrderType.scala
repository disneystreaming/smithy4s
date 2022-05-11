package smithy4s.example

import smithy4s.schema.Schema._

sealed trait OrderType extends scala.Product with scala.Serializable
object OrderType extends smithy4s.ShapeTag.Companion[OrderType] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "OrderType")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case class OnlineCase(online: OrderNumber) extends OrderType
  case class InStoreCase(inStore: InStoreOrder) extends OrderType

  object OnlineCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[OnlineCase] = bijection(OrderNumber.schema.addHints(hints), OnlineCase(_), _.online)
    val alt = schema.oneOf[OrderType]("online")
  }
  object InStoreCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[InStoreCase] = bijection(InStoreOrder.schema.addHints(hints), InStoreCase(_), _.inStore)
    val alt = schema.oneOf[OrderType]("inStore")
  }

  implicit val schema: smithy4s.Schema[OrderType] = union(
    OnlineCase.alt,
    InStoreCase.alt,
  ){
    case c : OnlineCase => OnlineCase.alt(c)
    case c : InStoreCase => InStoreCase.alt(c)
  }.withId(id).addHints(hints)
}