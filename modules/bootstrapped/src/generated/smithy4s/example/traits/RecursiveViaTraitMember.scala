package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class RecursiveViaTraitMember()

object RecursiveViaTraitMember extends ShapeTag.Companion[RecursiveViaTraitMember] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "recursiveViaTraitMember")

  val hints: Hints = Hints(
    ShapeId("smithy4s.example.traits", "traitWithMember") -> Document.obj("m" -> Document.fromString("foo")),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[RecursiveViaTraitMember] = constant(RecursiveViaTraitMember()).withId(id).addHints(hints)
}
