package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class SetInput(key: Key, value: Option[Value] = None)
object SetInput extends ShapeTag.Companion[SetInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "SetInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[SetInput] = struct(
    Key.schema.required[SetInput]("key", _.key).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    Value.schema.optional[SetInput]("value", _.value),
  ){
    SetInput.apply
  }.withId(id).addHints(hints)
}