package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

/** Our order types have different ways to identify a product
  * Except for preview orders, these don't have an ID
  * @param inStore For an InStoreOrder a location ID isn't needed
  */
sealed trait OrderType extends scala.Product with scala.Serializable {
  @inline final def widen: OrderType = this
}
object OrderType extends ShapeTag.Companion[OrderType] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderType")

  val hints: Hints = Hints(
    smithy.api.Documentation("Our order types have different ways to identify a product\nExcept for preview orders, these don\'t have an ID"),
  )

  case class OnlineCase(online: OrderNumber) extends OrderType
  /** For an InStoreOrder a location ID isn't needed */
  case class InStoreOrder(id: OrderNumber = smithy4s.example.OrderNumber(0), locationId: Option[String] = None) extends OrderType
  object InStoreOrder extends ShapeTag.Companion[InStoreOrder] {
    val id: ShapeId = ShapeId("smithy4s.example", "InStoreOrder")

    val hints: Hints = Hints(
      smithy.api.Documentation("For an InStoreOrder a location ID isn\'t needed"),
    )

    val schema: Schema[InStoreOrder] = struct(
      OrderNumber.schema.required[InStoreOrder]("id", _.id).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), smithy.api.Required()),
      string.optional[InStoreOrder]("locationId", _.locationId),
    ){
      InStoreOrder.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[OrderType]("inStore")
  }
  case object PreviewCase extends OrderType
  private val PreviewCaseAlt = Schema.constant(PreviewCase).oneOf[OrderType]("preview").addHints(hints)
  private val PreviewCaseAltWithValue = PreviewCaseAlt(PreviewCase)

  object OnlineCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OnlineCase] = bijection(OrderNumber.schema.addHints(hints), OnlineCase(_), _.online)
    val alt = schema.oneOf[OrderType]("online")
  }

  implicit val schema: Schema[OrderType] = union(
    OnlineCase.alt,
    InStoreOrder.alt,
    PreviewCaseAlt,
  ){
    case c: OnlineCase => OnlineCase.alt(c)
    case c: InStoreOrder => InStoreOrder.alt(c)
    case PreviewCase => PreviewCaseAltWithValue
  }.withId(id).addHints(hints)
}