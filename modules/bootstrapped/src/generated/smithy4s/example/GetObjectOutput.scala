package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GetObjectOutput(size: ObjectSize, data: Option[String] = None)

object GetObjectOutput extends ShapeTag.Companion[GetObjectOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetObjectOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetObjectOutput] = struct(
    ObjectSize.schema.required[GetObjectOutput]("size", _.size).addHints(smithy.api.HttpHeader("X-Size")),
    string.optional[GetObjectOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  ){
    GetObjectOutput.apply
  }.withId(id).addHints(hints)
}
