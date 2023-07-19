package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Three(three: String)
object Three extends ShapeTag.Companion[Three] {
  val id: ShapeId = ShapeId("smithy4s.example", "Three")

  val hints: Hints = Hints.empty

  object Optics {
    val three = Lens[Three, String](_.three)(n => a => a.copy(three = n))
  }

  implicit val schema: Schema[Three] = struct(
    string.required[Three]("three", _.three).addHints(smithy.api.Required()),
  ){
    Three.apply
  }.withId(id).addHints(hints)
}
