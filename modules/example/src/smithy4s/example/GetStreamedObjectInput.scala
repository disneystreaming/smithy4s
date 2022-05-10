package smithy4s.example

import smithy4s.schema.Schema._

case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput extends smithy4s.ShapeTag.Companion[GetStreamedObjectInput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetStreamedObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[GetStreamedObjectInput] = struct(
    string.required[GetStreamedObjectInput]("key", _.key).addHints(smithy.api.Required()),
  ){
    GetStreamedObjectInput.apply
  }.withId(id).addHints(hints)
}