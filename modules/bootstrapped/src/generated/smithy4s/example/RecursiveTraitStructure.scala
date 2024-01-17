package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RecursiveTraitStructure(name: Option[String] = None)

object RecursiveTraitStructure extends ShapeTag.Companion[RecursiveTraitStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveTraitStructure")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily

  implicit val schema: Schema[RecursiveTraitStructure] = recursive(struct(
    string.optional[RecursiveTraitStructure]("name", _.name).addHints(smithy4s.example.RecursiveTraitStructure(name = None)),
  ){
    RecursiveTraitStructure.apply
  }.withId(id).addHints(hints))
}
