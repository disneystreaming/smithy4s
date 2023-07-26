package smithy4s.example.test

import smithy.api.Error
import smithy.api.Required
import smithy.test.HttpResponseTestCase
import smithy.test.HttpResponseTests
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class SimpleError(expected: Int) extends Throwable {
}
object SimpleError extends ShapeTag.Companion[SimpleError] {

  val expected = int.required[SimpleError]("expected", _.expected, n => c => c.copy(expected = n)).addHints(Required())

  implicit val schema: Schema[SimpleError] = struct(
    expected,
  ){
    SimpleError.apply
  }
  .withId(ShapeId("smithy4s.example.test", "SimpleError"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
      HttpResponseTests(List(HttpResponseTestCase(id = "simple_error", protocol = "alloy#simpleRestJson", code = 400, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"expected\":-1}"), bodyMediaType = Some("application/json"), params = Some(smithy4s.Document.obj("expected" -> smithy4s.Document.fromDouble(-1.0d))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
    )
  )
}
