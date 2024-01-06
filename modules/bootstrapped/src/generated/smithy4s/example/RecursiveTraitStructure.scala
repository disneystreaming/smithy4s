package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.recursive
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class RecursiveTraitStructure(name: Option[String] = None)

object RecursiveTraitStructure extends ShapeTag.Companion[RecursiveTraitStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveTraitStructure")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[RecursiveTraitStructure] = recursive(struct(
    string.optional[RecursiveTraitStructure]("name", _.name).addHints(smithy4s.example.RecursiveTraitStructure(name = None)),
  ){
    RecursiveTraitStructure.apply
  }.withId(id).addHints(hints))
}
