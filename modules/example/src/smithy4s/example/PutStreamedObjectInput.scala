package smithy4s.example

import smithy4s.syntax._

case class PutStreamedObjectInput(key: String)
object PutStreamedObjectInput {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PutStreamedObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
  )

  val schema: smithy4s.Schema[PutStreamedObjectInput] = struct(
    string.required[PutStreamedObjectInput]("key", _.key).withHints(smithy.api.Required()),
  ){
    PutStreamedObjectInput.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[PutStreamedObjectInput]] = schematic.Static(schema)
}