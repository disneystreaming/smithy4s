package smithy4s.example

import OrderType.PreviewCaseAlt
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.struct
import _root_.smithy4s.schema.Schema.union
import smithy4s.schema.Schema.string

/** Our order types have different ways to identify a product
  * Except for preview orders, these don't have an ID 
  */
sealed trait OrderType extends _root_.scala.Product with _root_.scala.Serializable { self =>
  @inline final def widen: OrderType = this
  def $ordinal: Int

  object project {
    def online: Option[OrderNumber] = OrderType.OnlineCase.alt.project.lift(self).map(_.online)
    def inStore: Option[OrderType.InStoreOrder] = OrderType.InStoreOrder.alt.project.lift(self)
    def preview: Option[OrderType.PreviewCase.type] = PreviewCaseAlt.project.lift(self)
  }

  def accept[A](visitor: OrderType.Visitor[A]): A = this match {
    case value: OrderType.OnlineCase => visitor.online(value.online)
    case value: OrderType.InStoreOrder => visitor.inStore(value)
    case value: OrderType.PreviewCase.type => visitor.preview(value)
  }
}
object OrderType extends ShapeTag.Companion[OrderType] {

  def online(online: OrderNumber): OrderType = OnlineCase(online)
  /** For an InStoreOrder a location ID isn't needed */
  def inStoreOrder(id: OrderNumber, locationId: Option[String] = None):InStoreOrder = InStoreOrder(id, locationId)
  def preview(): OrderType = OrderType.PreviewCase

  val id: ShapeId = ShapeId("smithy4s.example", "OrderType")

  val hints: Hints = Hints(
    smithy.api.Documentation("Our order types have different ways to identify a product\nExcept for preview orders, these don\'t have an ID "),
  )

  final case class OnlineCase(online: OrderNumber) extends OrderType { final def $ordinal: Int = 0 }
  /** For an InStoreOrder a location ID isn't needed */
  final case class InStoreOrder(id: OrderNumber, locationId: Option[String] = None) extends OrderType {
    def $ordinal: Int = 1
  }

  object InStoreOrder extends ShapeTag.Companion[InStoreOrder] {
    val id: ShapeId = ShapeId("smithy4s.example", "InStoreOrder")

    val hints: Hints = Hints(
      smithy.api.Documentation("For an InStoreOrder a location ID isn\'t needed"),
    )

    val schema: Schema[InStoreOrder] = struct(
      OrderNumber.schema.required[InStoreOrder]("id", _.id),
      string.optional[InStoreOrder]("locationId", _.locationId),
    ){
      InStoreOrder.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[OrderType]("inStore")
  }
  case object PreviewCase extends OrderType { final def $ordinal: Int = 2 }
  private val PreviewCaseAlt = Schema.constant(OrderType.PreviewCase).oneOf[OrderType]("preview").addHints(hints)

  object OnlineCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OrderType.OnlineCase] = bijection(OrderNumber.schema.addHints(hints), OrderType.OnlineCase(_), _.online)
    val alt = schema.oneOf[OrderType]("online")
  }

  trait Visitor[A] {
    def online(value: OrderNumber): A
    def inStore(value: OrderType.InStoreOrder): A
    def preview(value: OrderType.PreviewCase.type): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def online(value: OrderNumber): A = default
      def inStore(value: OrderType.InStoreOrder): A = default
      def preview(value: OrderType.PreviewCase.type): A = default
    }
  }

  implicit val schema: Schema[OrderType] = union(
    OrderType.OnlineCase.alt,
    OrderType.InStoreOrder.alt,
    PreviewCaseAlt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
