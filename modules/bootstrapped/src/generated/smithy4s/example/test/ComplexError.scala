package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string

final case class ComplexError(value: Int, message: String, details: Option[ErrorDetails] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object ComplexError extends ShapeTag.Companion[ComplexError] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "ComplexError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(504),
    smithy.test.HttpResponseTests(List(smithy.test.HttpResponseTestCase(id = "complex_error", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), code = 504, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\",\"details\":{\"date\":123,\"location\":\"NYC\"}}"), bodyMediaType = Some("application/json"), params = Some(_root_.smithy4s.Document.obj("value" -> _root_.smithy4s.Document.fromDouble(-1.0d), "message" -> _root_.smithy4s.Document.fromString("some error message"), "details" -> _root_.smithy4s.Document.obj("date" -> _root_.smithy4s.Document.fromDouble(123.0d), "location" -> _root_.smithy4s.Document.fromString("NYC")))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None), smithy.test.HttpResponseTestCase(id = "complex_error_no_details", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), code = 504, authScheme = None, headers = None, forbidHeaders = None, requireHeaders = Some(List("X-Error-Type")), body = Some("{\"value\":-1,\"message\":\"some error message\"}"), bodyMediaType = Some("application/json"), params = Some(_root_.smithy4s.Document.obj("value" -> _root_.smithy4s.Document.fromDouble(-1.0d), "message" -> _root_.smithy4s.Document.fromString("some error message"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
  )

  implicit val schema: Schema[ComplexError] = struct(
    int.required[ComplexError]("value", _.value),
    string.required[ComplexError]("message", _.message),
    ErrorDetails.schema.optional[ComplexError]("details", _.details),
  ){
    ComplexError.apply
  }.withId(id).addHints(hints)
}
