package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class TraitWithMember(m: Option[M] = None)

object TraitWithMember extends ShapeTag.Companion[TraitWithMember] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "traitWithMember")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[TraitWithMember] = recursive(struct(
    M.schema.optional[TraitWithMember]("m", _.m),
  ){
    TraitWithMember.apply
  }.withId(id).addHints(hints))
}
