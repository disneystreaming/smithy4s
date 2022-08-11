package smithy4s.example.import_test

import smithy4s._
import smithy4s.schema.Schema._

case class OpOutput(output: String)
object OpOutput extends ShapeTag.Companion[OpOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.import_test", "OpOutput")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[OpOutput] = struct(
    string.required[OpOutput]("output", _.output).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    OpOutput.apply
  }.withId(id).addHints(hints)
}