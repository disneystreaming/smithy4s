package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Three(three: String)

object Three extends ShapeTag.Companion[Three] {
  val id: ShapeId = ShapeId("smithy4s.example", "Three")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(three: String): Three = Three(three)

  implicit val schema: Schema[Three] = struct(
    string.required[Three]("three", _.three),
  ){
    make
  }.withId(id).addHints(hints)
}
