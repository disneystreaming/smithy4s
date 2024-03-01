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

  // constructor using the original order from the spec
  private def make(four: Int): Four = Four(four)

  implicit val schema: Schema[Four] = struct(
    int.required[Four]("four", _.four),
  ){
    make
  }.withId(id).addHints(hints)
}
