package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloOutput(payload: SayHelloPayload, header1: String)

object SayHelloOutput extends ShapeTag.Companion[SayHelloOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(payload: SayHelloPayload, header1: String): SayHelloOutput = SayHelloOutput(payload, header1)

  implicit val schema: Schema[SayHelloOutput] = struct(
    SayHelloPayload.schema.required[SayHelloOutput]("payload", _.payload).addHints(smithy.api.HttpPayload()),
    string.required[SayHelloOutput]("header1", _.header1).addHints(smithy.api.HttpHeader("X-H1")),
  ){
    make
  }.withId(id).addHints(hints)
}
