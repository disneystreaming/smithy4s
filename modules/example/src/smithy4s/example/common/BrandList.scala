package smithy4s.example.common

import smithy4s._
import smithy4s.schema.Schema._

object BrandList extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.common", "BrandList")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema : Schema[BrandList] = bijection(underlyingSchema, asBijection)
}