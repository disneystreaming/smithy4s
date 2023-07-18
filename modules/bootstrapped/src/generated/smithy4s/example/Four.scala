package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Four(four: Int)
object Four extends ShapeTag.Companion[Four] {
  val id: ShapeId = ShapeId("smithy4s.example", "Four")

  val hints: Hints = Hints.empty

  object Lenses {
    val four = Lens[Four, Int](_.four)(n => a => a.copy(four = n))
  }

  implicit val schema: Schema[Four] = struct(
    int.required[Four]("four", _.four).addHints(smithy.api.Required()),
  ){
    Four.apply
  }.withId(id).addHints(hints)
}
