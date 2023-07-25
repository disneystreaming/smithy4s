package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetObjectOutput(size: ObjectSize, data: Option[String] = None)
object GetObjectOutput extends ShapeTag.Companion[GetObjectOutput] {
  val hints: Hints = Hints.empty

  val size = ObjectSize.schema.required[GetObjectOutput]("size", _.size).addHints(smithy.api.HttpHeader("X-Size"), smithy.api.Required())
  val data = string.optional[GetObjectOutput]("data", _.data).addHints(smithy.api.HttpPayload())

  implicit val schema: Schema[GetObjectOutput] = struct(
    size,
    data,
  ){
    GetObjectOutput.apply
  }.withId(ShapeId("smithy4s.example", "GetObjectOutput")).addHints(hints)
}
