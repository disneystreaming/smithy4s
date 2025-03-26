package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person3(item: String = "")

object Person3 extends ShapeTag.Companion[Person3] {
  val id: ShapeId = ShapeId("weather", "Person3")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(item: String): Person3 = Person3(item)

  implicit val schema: Schema[Person3] = struct(
    string.field[Person3]("item", _.item).addHints(smithy.api.Default(smithy4s.Document.fromString(""))),
  )(make).withId(id).addHints(hints)
}
