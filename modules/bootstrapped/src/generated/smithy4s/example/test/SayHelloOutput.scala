package smithy4s.example.test

import smithy.api.HttpHeader
import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloOutput(payload: SayHelloPayload, header1: String)
object SayHelloOutput extends ShapeTag.Companion[SayHelloOutput] {

  val payload: FieldLens[SayHelloOutput, SayHelloPayload] = SayHelloPayload.schema.required[SayHelloOutput]("payload", _.payload, n => c => c.copy(payload = n)).addHints(HttpPayload(), Required())
  val header1: FieldLens[SayHelloOutput, String] = string.required[SayHelloOutput]("header1", _.header1, n => c => c.copy(header1 = n)).addHints(HttpHeader("X-H1"), Required())

  implicit val schema: Schema[SayHelloOutput] = struct(
    payload,
    header1,
  ){
    SayHelloOutput.apply
  }
  .withId(ShapeId("smithy4s.example.test", "SayHelloOutput"))
}
