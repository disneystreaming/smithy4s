package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class SomeCollections(someList: List[String], someSet: Set[String], someMap: Map[String, String])
object SomeCollections extends ShapeTag.Companion[SomeCollections] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeCollections")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  object Lenses {
    val someList = Lens[SomeCollections, List[String]](_.someList)(n => a => a.copy(someList = n))
    val someSet = Lens[SomeCollections, Set[String]](_.someSet)(n => a => a.copy(someSet = n))
    val someMap = Lens[SomeCollections, Map[String, String]](_.someMap)(n => a => a.copy(someMap = n))
  }

  implicit val schema: Schema[SomeCollections] = recursive(struct(
    StringList.underlyingSchema.required[SomeCollections]("someList", _.someList).addHints(smithy.api.Required()),
    StringSet.underlyingSchema.required[SomeCollections]("someSet", _.someSet).addHints(smithy.api.Required()),
    StringMap.underlyingSchema.required[SomeCollections]("someMap", _.someMap).addHints(smithy.api.Required()),
  ){
    SomeCollections.apply
  }.withId(id).addHints(hints))
}
