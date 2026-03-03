package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ComplexError(value: Int, message: String, details: Option[ErrorDetails] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object ComplexError extends ShapeTag.Companion[ComplexError] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "ComplexError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(504),
    smithy.test.HttpResponseTests(List(smithy.test.HttpResponseTestCase(code = 504, id = "complex_error", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\",\"details\":{\"date\":123,\"location\":\"NYC\"}}"), bodyMediaType = Some("application/json"), authScheme = None, params = Some(smithy4s.Document.obj("value" -> smithy4s.Document.fromLong(-1L), "message" -> smithy4s.Document.fromString("some error message"), "details" -> smithy4s.Document.obj("date" -> smithy4s.Document.fromLong(123L), "location" -> smithy4s.Document.fromString("NYC")))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None), smithy.test.HttpResponseTestCase(code = 504, id = "complex_error_no_details", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\"}"), bodyMediaType = Some("application/json"), authScheme = None, params = Some(smithy4s.Document.obj("value" -> smithy4s.Document.fromLong(-1L), "message" -> smithy4s.Document.fromString("some error message"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
  ).lazily

  // constructor using the original order from the spec
  private def make(value: Int, message: String, details: Option[ErrorDetails]): ComplexError = ComplexError(value, message, details)

  implicit val schema: Schema[ComplexError] = struct[ComplexError](
    int.required[ComplexError]("value", _.value),
    string.required[ComplexError]("message", _.message),
    ErrorDetails.schema.optional[ComplexError]("details", _.details),
  )(make).withId(id).addHints(hints)
}
