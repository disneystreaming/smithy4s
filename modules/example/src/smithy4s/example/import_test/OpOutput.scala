package smithy4s.example.import_test

import smithy4s.schema.syntax._

case class OpOutput(output: String)
object OpOutput extends smithy4s.ShapeTag.Companion[OpOutput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example.import_test", "OpOutput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[OpOutput] = struct(
    string.required[OpOutput]("output", _.output).addHints(smithy.api.Required(), smithy.api.HttpPayload()),
  ){
    OpOutput.apply
  }.withId(id).addHints(hints)
}