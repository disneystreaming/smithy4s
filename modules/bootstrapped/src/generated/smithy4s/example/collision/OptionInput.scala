package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class OptionInput(value: Option[String] = None)
object OptionInput extends ShapeTag.Companion[OptionInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val value = String.schema.optional[OptionInput]("value", _.value)

  implicit val schema: Schema[OptionInput] = struct(
    value,
  ){
    OptionInput.apply
  }.withId(ShapeId("smithy4s.example.collision", "OptionInput")).addHints(hints)
}
