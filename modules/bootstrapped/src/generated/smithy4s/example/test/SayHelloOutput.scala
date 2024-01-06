package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class SayHelloOutput(payload: SayHelloPayload, header1: String)

object SayHelloOutput extends ShapeTag.Companion[SayHelloOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[SayHelloOutput] = struct(
    SayHelloPayload.schema.required[SayHelloOutput]("payload", _.payload).addHints(smithy.api.HttpPayload()),
    string.required[SayHelloOutput]("header1", _.header1).addHints(smithy.api.HttpHeader("X-H1")),
  ){
    SayHelloOutput.apply
  }.withId(id).addHints(hints)
}
