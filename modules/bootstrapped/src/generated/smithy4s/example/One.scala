package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class One(value: Option[String] = None)
object One extends ShapeTag.$Companion[One] {
  val $id: ShapeId = ShapeId("smithy4s.example", "One")

  val $hints: Hints = Hints.empty

  val value: FieldLens[One, Option[String]] = string.optional[One]("value", _.value, n => c => c.copy(value = n))

  implicit val $schema: Schema[One] = struct(
    value,
  ){
    One.apply
  }.withId($id).addHints($hints)
}