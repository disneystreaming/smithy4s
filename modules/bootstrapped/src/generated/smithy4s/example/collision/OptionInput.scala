package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class OptionInput(value: Option[String] = None)

object OptionInput extends ShapeTag.Companion[OptionInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "OptionInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[OptionInput] = struct(
    String.schema.optional[OptionInput]("value", _.value),
  ){
    OptionInput.apply
  }.withId(id).addHints(hints)
}
