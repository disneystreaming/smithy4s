package smithy4s.example.import_test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class OpOutput(output: String)

object OpOutput extends ShapeTag.Companion[OpOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.import_test", "OpOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[OpOutput] = struct(
    string.required[OpOutput]("output", _.output).addHints(smithy.api.HttpPayload()),
  ){
    OpOutput.apply
  }.withId(id).addHints(hints)
}
