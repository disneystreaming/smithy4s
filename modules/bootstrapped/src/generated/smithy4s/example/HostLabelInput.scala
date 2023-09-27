package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HostLabelInput(label1: String, label2: String, label3: HostLabelEnum)
object HostLabelInput extends ShapeTag.Companion[HostLabelInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "HostLabelInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[HostLabelInput] = struct(
    string.required[HostLabelInput]("label1", _.label1).addHints(smithy.api.HostLabel()),
    string.required[HostLabelInput]("label2", _.label2).addHints(smithy.api.HostLabel()),
    HostLabelEnum.schema.required[HostLabelInput]("label3", _.label3).addHints(smithy.api.HostLabel()),
  ){
    HostLabelInput.apply
  }.withId(id).addHints(hints)
}
