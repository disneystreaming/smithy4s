package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloOutput(payload: SayHelloPayload, header1: String)
object SayHelloOutput extends ShapeTag.Companion[SayHelloOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloOutput")

  val hints: Hints = Hints.empty

  object Lenses {
    val payload = Lens[SayHelloOutput, SayHelloPayload](_.payload)(n => a => a.copy(payload = n))
    val header1 = Lens[SayHelloOutput, String](_.header1)(n => a => a.copy(header1 = n))
  }

  implicit val schema: Schema[SayHelloOutput] = struct(
    SayHelloPayload.schema.required[SayHelloOutput]("payload", _.payload).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
    string.required[SayHelloOutput]("header1", _.header1).addHints(smithy.api.HttpHeader("X-H1"), smithy.api.Required()),
  ){
    SayHelloOutput.apply
  }.withId(id).addHints(hints)
}
