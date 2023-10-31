package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class StructureConstrainingEnum(letter: Option[Letters] = None)

object StructureConstrainingEnum extends ShapeTag.Companion[StructureConstrainingEnum] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureConstrainingEnum")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[StructureConstrainingEnum] = struct(
    Letters.schema.validated(smithy.api.Length(min = Some(2L), max = None)).optional[StructureConstrainingEnum]("letter", _.letter),
  ){
    StructureConstrainingEnum.apply
  }.withId(id).addHints(hints)
}
