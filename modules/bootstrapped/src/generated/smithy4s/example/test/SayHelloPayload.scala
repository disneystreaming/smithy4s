package smithy4s.example.test

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloPayload(result: String)
object SayHelloPayload extends ShapeTag.Companion[SayHelloPayload] {

  val result: FieldLens[SayHelloPayload, String] = string.required[SayHelloPayload]("result", _.result, n => c => c.copy(result = n)).addHints(Required())

  implicit val schema: Schema[SayHelloPayload] = struct(
    result,
  ){
    SayHelloPayload.apply
  }
  .withId(ShapeId("smithy4s.example.test", "SayHelloPayload"))
}
