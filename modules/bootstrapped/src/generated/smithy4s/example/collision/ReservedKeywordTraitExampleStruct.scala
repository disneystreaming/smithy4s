package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ReservedKeywordTraitExampleStruct(member: Option[String] = None)

object ReservedKeywordTraitExampleStruct extends ShapeTag.Companion[ReservedKeywordTraitExampleStruct] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedKeywordTraitExampleStruct")

  val hints: Hints = Hints(
    smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))),
    smithy4s.example.collision.ReservedKeywordUnionTrait.PackageCase(smithy4s.example.collision.PackageUnion.ClassCase(42).widen).widen,
  ).lazily

  implicit val schema: Schema[ReservedKeywordTraitExampleStruct] = struct(
    String.schema.optional[ReservedKeywordTraitExampleStruct]("member", _.member).addHints(smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))), smithy4s.example.collision.ReservedKeywordUnionTrait.PackageCase(smithy4s.example.collision.PackageUnion.ClassCase(42).widen).widen),
  ){
    ReservedKeywordTraitExampleStruct.apply
  }.withId(id).addHints(hints)
}
