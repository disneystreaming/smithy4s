package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Product()
object Product extends ShapeTag.Companion[Product] {
  val id: ShapeId = ShapeId("smithy4s.example", "Product")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Product] = constant(Product()).withId(id).addHints(hints)
}
