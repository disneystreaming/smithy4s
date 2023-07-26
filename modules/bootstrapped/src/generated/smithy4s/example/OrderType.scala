package smithy4s.example

import smithy.api.Documentation
import smithy.api.Required
import smithy4s.Bijection
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
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
  /** For an InStoreOrder a location ID isn't needed */
  final case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None) extends OrderType {
    def _ordinal: Int = 1
  }
  object InStoreOrder extends ShapeTag.Companion[InStoreOrder] {

    val id: FieldLens[InStoreOrder, OrderNumber] = OrderNumber.schema.required[InStoreOrder]("id", _.id, n => c => c.copy(id = n)).addHints(Required())
    val locationId: FieldLens[InStoreOrder, Option[String]] = string.optional[InStoreOrder]("locationId", _.locationId, n => c => c.copy(locationId = n))

    val schema: Schema[InStoreOrder] = struct(
      id,
      locationId,
    ){
      InStoreOrder.apply
    }
    .withId(ShapeId("smithy4s.example", "InStoreOrder"))
    .addHints(
      Documentation("For an InStoreOrder a location ID isn\'t needed"),
    )
  }
  case object PreviewCase extends OrderType {
    final def _ordinal: Int = 2
    val schema = Schema.constant(PreviewCase).addHints(Documentation("Our order types have different ways to identify a product\nExcept for preview orders, these don\'t have an ID"))
  }

  object OnlineCase {
    implicit val fromValue: Bijection[OrderNumber, OnlineCase] = Bijection(OnlineCase(_), _.online)
    implicit val toValue: Bijection[OnlineCase, OrderNumber] = fromValue.swap
    val schema: Schema[OnlineCase] = bijection(OrderNumber.schema, fromValue)
  }

  val online = OnlineCase.schema.oneOf[OrderType]("online")
  val inStore = InStoreOrder.schema.oneOf[OrderType]("inStore")
  val preview = PreviewCase.schema.oneOf[OrderType]("preview")

  implicit val schema: Schema[OrderType] = union(
    online,
    inStore,
    preview,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "OrderType"))
  .addHints(
    Documentation("Our order types have different ways to identify a product\nExcept for preview orders, these don\'t have an ID"),
  )
}
