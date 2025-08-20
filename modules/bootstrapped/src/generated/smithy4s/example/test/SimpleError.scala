package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class SimpleError(expected: Int) extends Smithy4sThrowable

object SimpleError extends ShapeTag.Companion[SimpleError] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SimpleError")

  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("client")),
    Hints.Binding.DynamicBinding(ShapeId("smithy.test", "httpResponseTests"), smithy4s.Document.array(smithy4s.Document.obj("id" -> smithy4s.Document.fromString("simple_error"), "protocol" -> smithy4s.Document.fromString("alloy#simpleRestJson"), "params" -> smithy4s.Document.obj("expected" -> smithy4s.Document.fromDouble(-1.0d)), "code" -> smithy4s.Document.fromDouble(400.0d), "body" -> smithy4s.Document.fromString("{\"expected\":-1}"), "bodyMediaType" -> smithy4s.Document.fromString("application/json"), "requireHeaders" -> smithy4s.Document.array(smithy4s.Document.fromString("X-Error-Type"))))),
  ).lazily

  // constructor using the original order from the spec
  private def make(expected: Int): SimpleError = SimpleError(expected)

  implicit val schema: Schema[SimpleError] = struct(
    int.required[SimpleError]("expected", _.expected),
  )(make).withId(id).addHints(hints)
}
