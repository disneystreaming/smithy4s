package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Three(three: String)
object Three extends ShapeTag.$Companion[Three] {
  val $id: ShapeId = ShapeId("smithy4s.example", "Three")

  val $hints: Hints = Hints.empty

  val three: FieldLens[Three, String] = string.required[Three]("three", _.three, n => c => c.copy(three = n)).addHints(Required())

  implicit val $schema: Schema[Three] = struct(
    three,
  ){
    Three.apply
  }.withId($id).addHints($hints)
}
