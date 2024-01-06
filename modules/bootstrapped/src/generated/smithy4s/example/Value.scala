package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Value(value: String)

object Value extends ShapeTag.Companion[Value] {
  val id: ShapeId = ShapeId("smithy4s.example", "Value")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Value] = struct(
    string.required[Value]("value", _.value),
  ){
    Value.apply
  }.withId(id).addHints(hints)
}
