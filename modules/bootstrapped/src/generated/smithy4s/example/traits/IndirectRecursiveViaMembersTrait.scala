package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** A trait that has indirect recursive references to it via members. */
final case class IndirectRecursiveViaMembersTrait(member: Option[RecursiveMember] = None)

object IndirectRecursiveViaMembersTrait extends ShapeTag.Companion[IndirectRecursiveViaMembersTrait] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "indirectRecursiveViaMembersTrait")

  val hints: Hints = Hints(
    smithy.api.Documentation("A trait that has indirect recursive references to it via members."),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[IndirectRecursiveViaMembersTrait] = recursive(struct(
    RecursiveMember.schema.optional[IndirectRecursiveViaMembersTrait]("member", _.member),
  ){
    IndirectRecursiveViaMembersTrait.apply
  }.withId(id).addHints(hints))
}
