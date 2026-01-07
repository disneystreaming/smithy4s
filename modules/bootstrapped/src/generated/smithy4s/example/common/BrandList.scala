package smithy4s.example.common

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object BrandList extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.common", "BrandList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema: Schema[BrandList] = bijection(underlyingSchema, asBijection)
}
