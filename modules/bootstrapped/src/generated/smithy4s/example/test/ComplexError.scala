package smithy4s.example.test

import smithy.api.Error
import smithy.api.HttpError
import smithy.api.Required
import smithy.test.HttpResponseTestCase
import smithy.test.HttpResponseTests
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ComplexError(value: Int, message: String, details: Option[ErrorDetails] = None) extends Throwable {
  override def getMessage(): String = message
}
object ComplexError extends ShapeTag.Companion[ComplexError] {

  val value = int.required[ComplexError]("value", _.value, n => c => c.copy(value = n)).addHints(Required())
  val message = string.required[ComplexError]("message", _.message, n => c => c.copy(message = n)).addHints(Required())
  val details = ErrorDetails.schema.optional[ComplexError]("details", _.details, n => c => c.copy(details = n))

  implicit val schema: Schema[ComplexError] = struct(
    value,
    message,
    details,
  ){
    ComplexError.apply
  }
  .withId(ShapeId("smithy4s.example.test", "ComplexError"))
  .addHints(
    Hints(
      Error.SERVER.widen,
      HttpError(504),
      HttpResponseTests(List(HttpResponseTestCase(id = "complex_error", protocol = "alloy#simpleRestJson", code = 504, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\",\"details\":{\"date\":123,\"location\":\"NYC\"}}"), bodyMediaType = Some("application/json"), params = Some(smithy4s.Document.obj("value" -> smithy4s.Document.fromDouble(-1.0d), "message" -> smithy4s.Document.fromString("some error message"), "details" -> smithy4s.Document.obj("date" -> smithy4s.Document.fromDouble(123.0d), "location" -> smithy4s.Document.fromString("NYC")))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None), HttpResponseTestCase(id = "complex_error_no_details", protocol = "alloy#simpleRestJson", code = 504, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\"}"), bodyMediaType = Some("application/json"), params = Some(smithy4s.Document.obj("value" -> smithy4s.Document.fromDouble(-1.0d), "message" -> smithy4s.Document.fromString("some error message"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
    )
  )
}
