package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

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
