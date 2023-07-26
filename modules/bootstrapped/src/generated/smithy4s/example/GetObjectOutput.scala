package smithy4s.example

import smithy.api.HttpHeader
import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetObjectOutput(size: ObjectSize, data: Option[String] = None)
object GetObjectOutput extends ShapeTag.Companion[GetObjectOutput] {

  val size: FieldLens[GetObjectOutput, ObjectSize] = ObjectSize.schema.required[GetObjectOutput]("size", _.size, n => c => c.copy(size = n)).addHints(HttpHeader("X-Size"), Required())
  val data: FieldLens[GetObjectOutput, Option[String]] = string.optional[GetObjectOutput]("data", _.data, n => c => c.copy(data = n)).addHints(HttpPayload())

  implicit val schema: Schema[GetObjectOutput] = struct(
    size,
    data,
  ){
    GetObjectOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetObjectOutput"))
}
