package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class One(value: Option[String] = None)

object One extends ShapeTag.Companion[One] {
  val id: ShapeId = ShapeId("smithy4s.example", "One")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(value: Option[String]): One = One(value)

  implicit val schema: Schema[One] = struct(
    string.optional[One]("value", _.value),
  )(make).withId(id).addHints(hints)
}
