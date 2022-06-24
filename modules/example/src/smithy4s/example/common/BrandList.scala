package smithy4s.example.common

import smithy4s.Newtype
import smithy4s.schema.Schema._

object BrandList extends Newtype[List[String]] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example.common", "BrandList")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[BrandList] = bijection(underlyingSchema, BrandList(_), (_ : BrandList).value)
}