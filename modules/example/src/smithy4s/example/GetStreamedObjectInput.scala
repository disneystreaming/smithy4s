package smithy4s.example

import smithy4s.syntax._

case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput {
  def namespace: String = NAMESPACE
  val name: String = "GetStreamedObjectInput"

  val hints : smithy4s.Hints = smithy4s.Hints()

  val schema: smithy4s.Schema[GetStreamedObjectInput] = struct(
    string.required[GetStreamedObjectInput]("key", _.key).withHints(smithy.api.Required()),
  ){
    GetStreamedObjectInput.apply
  }
  implicit val staticSchema : schematic.Static[smithy4s.Schema[GetStreamedObjectInput]] = schematic.Static(schema)
}