package smithy4s.example

import smithy4s.example.ObjectSize.schema
import smithy4s.schema.Schema._

case class GetObjectOutput(size: ObjectSize, data: Option[String]=None)
object GetObjectOutput extends smithy4s.ShapeTag.Companion[GetObjectOutput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetObjectOutput")
  
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  
  implicit val schema: smithy4s.Schema[GetObjectOutput] = struct(
    ObjectSize.schema.required[GetObjectOutput]("size", _.size).addHints(smithy.api.HttpHeader("X-Size"), smithy.api.Required()),
    string.optional[GetObjectOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    GetObjectOutput.apply
  }.withId(id).addHints(hints)
}