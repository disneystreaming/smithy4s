package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class SomeCollections(someList: List[String], someSet: Set[String], someMap: Map[String, String])
object SomeCollections extends ShapeTag.Companion[SomeCollections] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeCollections")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[SomeCollections] = recursive(struct(
    SomeList.underlyingSchema.required[SomeCollections]("someList", _.someList).addHints(smithy.api.Required()),
    SomeSet.underlyingSchema.required[SomeCollections]("someSet", _.someSet).addHints(smithy.api.Required()),
    SomeMap.underlyingSchema.required[SomeCollections]("someMap", _.someMap).addHints(smithy.api.Required()),
  ){
    SomeCollections.apply
  }.withId(id).addHints(hints))
}
