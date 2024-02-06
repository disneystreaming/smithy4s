package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class Scala3ReservedKeywords(_export: Option[String] = None, _enum: Option[String] = None)

object Scala3ReservedKeywords extends ShapeTag.Companion[Scala3ReservedKeywords] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "Scala3ReservedKeywords")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Scala3ReservedKeywords] = struct(
    String.schema.optional[Scala3ReservedKeywords]("export", _._export),
    String.schema.optional[Scala3ReservedKeywords]("enum", _._enum),
  ){
    Scala3ReservedKeywords.apply
  }.withId(id).addHints(hints)
}
