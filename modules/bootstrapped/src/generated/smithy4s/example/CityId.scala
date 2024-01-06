package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object CityId extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "CityId")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern(s"^[A-Za-z0-9 ]+$$"))
  implicit val schema: Schema[CityId] = bijection(underlyingSchema, asBijection)
}
