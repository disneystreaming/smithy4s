package weather

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Nullable.Null
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person5(item: Nullable[String] = Null)

object Person5 extends ShapeTag.Companion[Person5] {
  val id: ShapeId = ShapeId("weather", "Person5")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(item: Nullable[String]): Person5 = Person5(item)

  implicit val schema: Schema[Person5] = struct(
    string.nullable.field[Person5]("item", _.item).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
  )(make).withId(id).addHints(hints)
}
