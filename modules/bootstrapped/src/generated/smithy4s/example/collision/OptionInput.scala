package smithy4s.example.collision

import smithy.api.Input
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class OptionInput(value: Option[String] = None)
object OptionInput extends ShapeTag.$Companion[OptionInput] {
  val $id: ShapeId = ShapeId("smithy4s.example.collision", "OptionInput")

  val $hints: Hints = Hints(
    Input(),
  )

  val value: FieldLens[OptionInput, Option[String]] = String.$schema.optional[OptionInput]("value", _.value, n => c => c.copy(value = n))

  implicit val $schema: Schema[OptionInput] = struct(
    value,
  ){
    OptionInput.apply
  }.withId($id).addHints($hints)
}
