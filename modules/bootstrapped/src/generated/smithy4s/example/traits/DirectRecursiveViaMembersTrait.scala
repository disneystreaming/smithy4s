package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** A trait that has direct recursive references to itself via members. */
final case class DirectRecursiveViaMembersTrait(member: Option[String] = None)

object DirectRecursiveViaMembersTrait extends ShapeTag.Companion[DirectRecursiveViaMembersTrait] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "directRecursiveViaMembersTrait")

  val hints: Hints = Hints(
    smithy.api.Documentation("A trait that has direct recursive references to itself via members."),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[DirectRecursiveViaMembersTrait] = recursive(struct(
    string.optional[DirectRecursiveViaMembersTrait]("member", _.member).addHints(smithy4s.example.traits.DirectRecursiveViaMembersTrait(member = None)),
  ){
    DirectRecursiveViaMembersTrait.apply
  }.withId(id).addHints(hints))
}
