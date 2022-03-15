package smithy4s.example

import smithy4s.schema.syntax._

case class PutStreamedObjectInput(key: String)
object PutStreamedObjectInput extends smithy4s.ShapeTag.Companion[PutStreamedObjectInput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PutStreamedObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[PutStreamedObjectInput] = struct(
    string.required[PutStreamedObjectInput]("key", _.key).addHints(smithy.api.Required()),
  ){
    PutStreamedObjectInput.apply
  }.withId(id).addHints(hints)
}