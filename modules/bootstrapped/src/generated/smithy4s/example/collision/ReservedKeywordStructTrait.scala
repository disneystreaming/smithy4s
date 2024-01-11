package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class ReservedKeywordStructTrait(_implicit: String, _package: Option[Packagee] = None)

object ReservedKeywordStructTrait extends ShapeTag.Companion[ReservedKeywordStructTrait] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "reservedKeywordStructTrait")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[ReservedKeywordStructTrait] = recursive(struct(
    String.schema.required[ReservedKeywordStructTrait]("implicit", _._implicit),
    Packagee.schema.optional[ReservedKeywordStructTrait]("package", _._package),
  ){
    ReservedKeywordStructTrait.apply
  }.withId(id).addHints(hints))
}
