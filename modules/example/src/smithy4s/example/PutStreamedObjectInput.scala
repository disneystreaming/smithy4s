package smithy4s.example

import smithy4s.syntax._

case class PutStreamedObjectInput(key: String)
object PutStreamedObjectInput {
  def namespace: String = NAMESPACE
  val name: String = "PutStreamedObjectInput"

  val hints : smithy4s.Hints = smithy4s.Hints()

  val schema: smithy4s.Schema[PutStreamedObjectInput] = struct(
    string.required[PutStreamedObjectInput]("key", _.key).withHints(smithy.api.Required()),
  ){
    PutStreamedObjectInput.apply
  }
  implicit val staticSchema : schematic.Static[smithy4s.Schema[PutStreamedObjectInput]] = schematic.Static(schema)
}