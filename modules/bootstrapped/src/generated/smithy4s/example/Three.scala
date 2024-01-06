package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Three(three: String)

object Three extends ShapeTag.Companion[Three] {
  val id: ShapeId = ShapeId("smithy4s.example", "Three")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Three] = struct(
    string.required[Three]("three", _.three),
  ){
    Three.apply
  }.withId(id).addHints(hints)
}
