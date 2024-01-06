package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HeadRequestOutput(test: String)

object HeadRequestOutput extends ShapeTag.Companion[HeadRequestOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeadRequestOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[HeadRequestOutput] = struct(
    string.required[HeadRequestOutput]("test", _.test).addHints(smithy.api.HttpHeader("Test")),
  ){
    HeadRequestOutput.apply
  }.withId(id).addHints(hints)
}
