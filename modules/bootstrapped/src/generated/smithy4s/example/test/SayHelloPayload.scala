package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class SayHelloPayload(result: String)

object SayHelloPayload extends ShapeTag.Companion[SayHelloPayload] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloPayload")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[SayHelloPayload] = struct(
    string.required[SayHelloPayload]("result", _.result),
  ){
    SayHelloPayload.apply
  }.withId(id).addHints(hints)
}
