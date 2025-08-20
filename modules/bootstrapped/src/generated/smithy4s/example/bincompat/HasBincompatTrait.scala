package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class HasBincompatTrait()

object HasBincompatTrait extends ShapeTag.Companion[HasBincompatTrait] {
  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "HasBincompatTrait")

  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.bincompat", "BincompatFriendlyTraitStruct"), smithy4s.Document.obj("base1" -> smithy4s.Document.fromString("b1"), "base2" -> smithy4s.Document.fromString("b2"), "added3_1" -> smithy4s.Document.fromString("b4"))),
  ).lazily


  implicit val schema: Schema[HasBincompatTrait] = constant(HasBincompatTrait()).withId(id).addHints(hints)
}
