package smithy4s.example

import smithy4s.syntax._

case class GetStreamedObjectOutput()
object GetStreamedObjectOutput {
  def namespace: String = NAMESPACE
  val name: String = "GetStreamedObjectOutput"

  val hints : smithy4s.Hints = smithy4s.Hints()

  val schema: smithy4s.Schema[GetStreamedObjectOutput] = constant(GetStreamedObjectOutput()).withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[GetStreamedObjectOutput]] = schematic.Static(schema)
}