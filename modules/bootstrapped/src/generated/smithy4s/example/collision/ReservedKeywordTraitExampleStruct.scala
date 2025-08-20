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
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordStructTrait"), smithy4s.Document.obj("implicit" -> smithy4s.Document.fromString("demo"), "package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d)))),
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordUnionTrait"), smithy4s.Document.obj("package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d)))),
  ).lazily

  // constructor using the original order from the spec
  private def make(member: Option[String]): ReservedKeywordTraitExampleStruct = ReservedKeywordTraitExampleStruct(member)

  implicit val schema: Schema[ReservedKeywordTraitExampleStruct] = struct(
    String.schema.optional[ReservedKeywordTraitExampleStruct]("member", _.member).addHints(Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordStructTrait"), smithy4s.Document.obj("implicit" -> smithy4s.Document.fromString("demo"), "package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d)))), Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordUnionTrait"), smithy4s.Document.obj("package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d))))),
  )(make).withId(id).addHints(hints)
}
