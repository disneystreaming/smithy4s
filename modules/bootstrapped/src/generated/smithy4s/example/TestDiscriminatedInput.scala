package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestDiscriminatedInput(key: String)
object TestDiscriminatedInput extends ShapeTag.Companion[TestDiscriminatedInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestDiscriminatedInput")

  val hints: Hints = Hints.empty

  object Lenses {
    val key = Lens[TestDiscriminatedInput, String](_.key)(n => a => a.copy(key = n))
  }

  implicit val schema: Schema[TestDiscriminatedInput] = struct(
    string.required[TestDiscriminatedInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    TestDiscriminatedInput.apply
  }.withId(id).addHints(hints)
}
