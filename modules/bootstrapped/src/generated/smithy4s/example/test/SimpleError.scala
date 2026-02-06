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
    Hints.dynamic(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("client")),
    smithy.test.HttpResponseTests(List(smithy.test.HttpResponseTestCase(code = 400, id = "simple_error", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"expected\":-1}"), bodyMediaType = Some("application/json"), authScheme = None, params = Some(smithy4s.Document.obj("expected" -> smithy4s.Document.fromLong(-1))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
  ).lazily

  // constructor using the original order from the spec
  private def make(expected: Int): SimpleError = SimpleError(expected)

  implicit val schema: Schema[SimpleError] = struct(
    int.required[SimpleError]("expected", _.expected),
  )(make).withId(id).addHints(hints)
}
