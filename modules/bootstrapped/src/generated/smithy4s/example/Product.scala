package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class Product()

object Product extends ShapeTag.Companion[Product] {
  val id: ShapeId = ShapeId("smithy4s.example", "Product")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Product] = constant(Product()).withId(id).addHints(hints)
}
