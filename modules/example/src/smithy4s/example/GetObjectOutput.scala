package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class GetObjectOutput(size: ObjectSize = smithy4s.example.ObjectSize(0), data: Option[String] = None)
object GetObjectOutput extends ShapeTag.Companion[GetObjectOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetObjectOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetObjectOutput] = struct(
    ObjectSize.schema.required[GetObjectOutput]("size", _.size).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), smithy.api.Required(), smithy.api.HttpHeader("X-Size")),
    string.optional[GetObjectOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    GetObjectOutput.apply
  }.withId(id).addHints(hints)
}
