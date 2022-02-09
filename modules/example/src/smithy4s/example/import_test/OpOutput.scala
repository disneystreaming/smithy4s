package smithy4s.example.import_test

import smithy4s.syntax._

case class OpOutput(output: String)
object OpOutput extends smithy4s.ShapeTag.Companion[OpOutput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example.import_test", "OpOutput")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
  )

  val schema: smithy4s.Schema[OpOutput] = struct(
    string.required[OpOutput]("output", _.output).withHints(smithy.api.Required(), smithy.api.HttpPayload()),
  ){
    OpOutput.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[OpOutput]] = schematic.Static(schema)
}