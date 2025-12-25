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
    smithy4s.example.bincompat.BincompatFriendlyTraitStruct(base1 = "b1", base2 = "b2", added2_1 = "woop2_1", added3_1 = "b4", base3 = None),
  ).lazily


  implicit val schema: Schema[HasBincompatTrait] = constant(HasBincompatTrait()).withId(id).addHints(hints)
}
