package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class One(value: Option[String] = None)

object One extends ShapeTag.Companion[One] {
  val id: ShapeId = ShapeId("smithy4s.example", "One")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[One] = struct(
    string.optional[One]("value", _.value),
  ){
    One.apply
  }.withId(id).addHints(hints)
}
