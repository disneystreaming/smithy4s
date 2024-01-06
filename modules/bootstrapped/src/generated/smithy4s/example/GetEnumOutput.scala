package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GetEnumOutput(result: Option[String] = None)

object GetEnumOutput extends ShapeTag.Companion[GetEnumOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetEnumOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetEnumOutput] = struct(
    string.optional[GetEnumOutput]("result", _.result),
  ){
    GetEnumOutput.apply
  }.withId(id).addHints(hints)
}
