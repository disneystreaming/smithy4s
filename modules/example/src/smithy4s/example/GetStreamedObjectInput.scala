package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput extends ShapeTag.Companion[GetStreamedObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObjectInput")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[GetStreamedObjectInput] = struct(
    string.required[GetStreamedObjectInput]("key", _.key).addHints(smithy.api.Required()),
  ){
    GetStreamedObjectInput.apply
  }.withId(id).addHints(hints)
}