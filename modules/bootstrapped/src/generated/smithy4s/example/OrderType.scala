package smithy4s.example

import smithy.api.Documentation
import smithy.api.Required
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
  */
sealed trait OrderType extends scala.Product with scala.Serializable {
  @inline final def widen: OrderType = this
  def _ordinal: Int
}
object OrderType extends ShapeTag.Companion[OrderType] {
  final case class OnlineCase(online: OrderNumber) extends OrderType { final def _ordinal: Int = 0 }
  def online(online:OrderNumber): OrderType = OnlineCase(online)
  /** For an InStoreOrder a location ID isn't needed */
  final case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None) extends OrderType {
    def _ordinal: Int = 1
  }
  object InStoreOrder extends ShapeTag.Companion[InStoreOrder] {

    val id = OrderNumber.schema.required[InStoreOrder]("id", _.id, n => c => c.copy(id = n)).addHints(Required())
    val locationId = string.optional[InStoreOrder]("locationId", _.locationId, n => c => c.copy(locationId = n))

    val schema: Schema[InStoreOrder] = struct(
      id,
      locationId,
    ){
      InStoreOrder.apply
    }
    .withId(ShapeId("smithy4s.example", "InStoreOrder"))
    .addHints(
      Hints(
        Documentation("For an InStoreOrder a location ID isn\'t needed"),
      )
    )

    val alt = schema.oneOf[OrderType]("inStore")
  }
  case object PreviewCase extends OrderType { final def _ordinal: Int = 2 }
  def preview(): OrderType = PreviewCase
  private val PreviewCaseAlt = Schema.constant(PreviewCase).oneOf[OrderType]("preview")
  .addHints(
    Hints(
      Documentation("Our order types have different ways to identify a product\nExcept for preview orders, these don\'t have an ID"),
    )
  )

  object OnlineCase {
    val schema: Schema[OnlineCase] = bijection(OrderNumber.schema
    .addHints(
      Hints.empty
    )
    , OnlineCase(_), _.online)
    val alt = schema.oneOf[OrderType]("online")
  }

  implicit val schema: Schema[OrderType] = union(
    OnlineCase.alt,
    InStoreOrder.alt,
    PreviewCaseAlt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "OrderType"))
  .addHints(
    Hints(
      Documentation("Our order types have different ways to identify a product\nExcept for preview orders, these don\'t have an ID"),
    )
  )
}
