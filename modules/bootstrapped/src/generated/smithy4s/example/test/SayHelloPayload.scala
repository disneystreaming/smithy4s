package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloPayload(result: String)
object SayHelloPayload extends ShapeTag.Companion[SayHelloPayload] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloPayload")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[SayHelloPayload] = struct(
    string.required[SayHelloPayload]("result", _.result).addHints(smithy.api.Required()),
  ){
    SayHelloPayload.apply
  }.withId(id).addHints(hints)
}
