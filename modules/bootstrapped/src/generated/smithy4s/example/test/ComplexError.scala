package smithy4s.example.test

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
  val id: ShapeId = ShapeId("smithy4s.example.test", "ComplexError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(504),
    smithy.test.HttpResponseTests(List(smithy.test.HttpResponseTestCase(id = "complex_error", protocol = "alloy#simpleRestJson", code = 504, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\",\"details\":{\"date\":123,\"location\":\"NYC\"}}"), bodyMediaType = Some("application/json"), params = Some(smithy4s.Document.obj("value" -> smithy4s.Document.fromDouble(-1.0d), "message" -> smithy4s.Document.fromString("some error message"), "details" -> smithy4s.Document.obj("date" -> smithy4s.Document.fromDouble(123.0d), "location" -> smithy4s.Document.fromString("NYC")))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None), smithy.test.HttpResponseTestCase(id = "complex_error_no_details", protocol = "alloy#simpleRestJson", code = 504, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\"}"), bodyMediaType = Some("application/json"), params = Some(smithy4s.Document.obj("value" -> smithy4s.Document.fromDouble(-1.0d), "message" -> smithy4s.Document.fromString("some error message"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
  )

  implicit val schema: Schema[ComplexError] = struct(
    int.required[ComplexError]("value", _.value).addHints(smithy.api.Required()),
    string.required[ComplexError]("message", _.message).addHints(smithy.api.Required()),
    ErrorDetails.schema.optional[ComplexError]("details", _.details),
  ){
    ComplexError.apply
  }.withId(id).addHints(hints)
}
