package smithy4s.example.import_test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class OpOutput(output: String)
object OpOutput extends ShapeTag.Companion[OpOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.import_test", "OpOutput")

  val hints: Hints = Hints.empty

  val output = string.required[OpOutput]("output", _.output).addHints(smithy.api.HttpPayload(), smithy.api.Required())

  implicit val schema: Schema[OpOutput] = struct(
    output,
  ){
    OpOutput.apply
  }.withId(id).addHints(hints)
}
