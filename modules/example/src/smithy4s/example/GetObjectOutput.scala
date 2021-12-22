package smithy4s.example

import smithy4s.syntax._

case class GetObjectOutput(size: ObjectSize, data: Option[String] = None)
object GetObjectOutput {
  def namespace: String = NAMESPACE
  val name: String = "GetObjectOutput"

  val hints : smithy4s.Hints = smithy4s.Hints()

  val schema: smithy4s.Schema[GetObjectOutput] = struct(
    ObjectSize.schema.required[GetObjectOutput]("size", _.size).withHints(smithy.api.HttpHeader("X-Size"), smithy.api.Required()),
    string.optional[GetObjectOutput]("data", _.data).withHints(smithy.api.HttpPayload()),
  ){
    GetObjectOutput.apply
  }
  implicit val staticSchema : schematic.Static[smithy4s.Schema[GetObjectOutput]] = schematic.Static(schema)
}