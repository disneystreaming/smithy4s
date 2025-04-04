package weather

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Nullable.Value
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person4(item: Nullable[String] = Value(""))

object Person4 extends ShapeTag.Companion[Person4] {
  val id: ShapeId = ShapeId("weather", "Person4")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(item: Nullable[String]): Person4 = Person4(item)

  implicit val schema: Schema[Person4] = struct(
    string.nullable.field[Person4]("item", _.item).addHints(smithy.api.Default(smithy4s.Document.fromString(""))),
  )(make).withId(id).addHints(hints)
}
