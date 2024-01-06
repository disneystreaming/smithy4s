package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

final case class SimpleError(expected: Int) extends Smithy4sThrowable {
}

object SimpleError extends ShapeTag.Companion[SimpleError] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SimpleError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.test.HttpResponseTests(List(smithy.test.HttpResponseTestCase(id = "simple_error", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), code = 400, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"expected\":-1}"), bodyMediaType = Some("application/json"), params = Some(_root_.smithy4s.Document.obj("expected" -> _root_.smithy4s.Document.fromDouble(-1.0d))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
  )

  implicit val schema: Schema[SimpleError] = struct(
    int.required[SimpleError]("expected", _.expected),
  ){
    SimpleError.apply
  }.withId(id).addHints(hints)
}
