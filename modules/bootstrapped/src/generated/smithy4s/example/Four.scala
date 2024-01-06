package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

final case class Four(four: Int)

object Four extends ShapeTag.Companion[Four] {
  val id: ShapeId = ShapeId("smithy4s.example", "Four")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Four] = struct(
    int.required[Four]("four", _.four),
  ){
    Four.apply
  }.withId(id).addHints(hints)
}
