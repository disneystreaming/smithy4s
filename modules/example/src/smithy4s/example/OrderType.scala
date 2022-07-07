package smithy4s.example

import smithy4s.schema.Schema._

sealed trait OrderType extends scala.Product with scala.Serializable {
  @inline final def widen: OrderType = this
}
object OrderType extends smithy4s.ShapeTag.Companion[OrderType] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "OrderType")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  case class OnlineCase(online: OrderNumber) extends OrderType
  case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None) extends OrderType
  object InStoreOrder extends smithy4s.ShapeTag.Companion[InStoreOrder] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "InStoreOrder")

    val hints : smithy4s.Hints = smithy4s.Hints.empty

    val schema: smithy4s.Schema[InStoreOrder] = struct(
      OrderNumber.schema.required[InStoreOrder]("id", _.id).addHints(smithy.api.Required()),
      string.optional[InStoreOrder]("locationId", _.locationId),
    ){
      InStoreOrder.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[OrderType]("inStore")
  }
  case object PreviewCase extends OrderType
  private val PreviewCaseAlt = smithy4s.Schema.constant(PreviewCase).oneOf[OrderType]("preview").addHints(hints)
  private val PreviewCaseAltWithValue = PreviewCaseAlt(PreviewCase)

  object OnlineCase {
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    val schema: smithy4s.Schema[OnlineCase] = bijection(OrderNumber.schema.addHints(hints), OnlineCase(_), _.online)
    val alt = schema.oneOf[OrderType]("online")
  }

  implicit val schema: smithy4s.Schema[OrderType] = union(
    OnlineCase.alt,
    InStoreOrder.alt,
    PreviewCaseAlt,
  ){
    case c : OnlineCase => OnlineCase.alt(c)
    case c : InStoreOrder => InStoreOrder.alt(c)
    case PreviewCase => PreviewCaseAltWithValue
  }.withId(id).addHints(hints)
}