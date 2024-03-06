package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Value(value: String)

object Value extends ShapeTag.Companion[Value] {
  val id: ShapeId = ShapeId("smithy4s.example", "Value")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(value: String): Value = Value(value)

  implicit val schema: Schema[Value] = struct(
    string.required[Value]("value", _.value),
  )(make).withId(id).addHints(hints)
}
