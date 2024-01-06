package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.recursive
import _root_.smithy4s.schema.Schema.struct

final case class SomeCollections(someList: List[String], someSet: Set[String], someMap: Map[String, String])

object SomeCollections extends ShapeTag.Companion[SomeCollections] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeCollections")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[SomeCollections] = recursive(struct(
    StringList.underlyingSchema.required[SomeCollections]("someList", _.someList),
    StringSet.underlyingSchema.required[SomeCollections]("someSet", _.someSet),
    StringMap.underlyingSchema.required[SomeCollections]("someMap", _.someMap),
  ){
    SomeCollections.apply
  }.withId(id).addHints(hints))
}
