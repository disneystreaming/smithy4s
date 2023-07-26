package smithy4s.example

import smithy.api.Required
import smithy.api.Trait
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class SomeCollections(someList: List[String], someSet: Set[String], someMap: Map[String, String])
object SomeCollections extends ShapeTag.$Companion[SomeCollections] {
  val $id: ShapeId = ShapeId("smithy4s.example", "SomeCollections")

  val $hints: Hints = Hints(
    Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val $schema: Schema[SomeCollections] = recursive(struct(
    someList,
    someSet,
    someMap,
  ){
    SomeCollections.apply
  }.withId($id).addHints($hints))

  val someList: FieldLens[SomeCollections, List[String]] = StringList.underlyingSchema.required[SomeCollections]("someList", _.someList, n => c => c.copy(someList = n)).addHints(Required())
  val someSet: FieldLens[SomeCollections, Set[String]] = StringSet.underlyingSchema.required[SomeCollections]("someSet", _.someSet, n => c => c.copy(someSet = n)).addHints(Required())
  val someMap: FieldLens[SomeCollections, Map[String, String]] = StringMap.underlyingSchema.required[SomeCollections]("someMap", _.someMap, n => c => c.copy(someMap = n)).addHints(Required())
}
