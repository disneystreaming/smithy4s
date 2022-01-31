package smithy4s.example

import smithy4s.syntax._

case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput extends smithy4s.ShapeTag.Companion[GetStreamedObjectInput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetStreamedObjectInput")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
  )

  val schema: smithy4s.Schema[GetStreamedObjectInput] = struct(
    string.required[GetStreamedObjectInput]("key", _.key).withHints(smithy.api.Required()),
  ){
    GetStreamedObjectInput.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[GetStreamedObjectInput]] = schematic.Static(schema)
}