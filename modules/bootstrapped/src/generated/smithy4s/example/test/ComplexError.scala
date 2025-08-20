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
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("server")),
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "httpError"), smithy4s.Document.fromDouble(504.0d)),
    Hints.Binding.DynamicBinding(ShapeId("smithy.test", "httpResponseTests"), smithy4s.Document.array(smithy4s.Document.obj("id" -> smithy4s.Document.fromString("complex_error"), "protocol" -> smithy4s.Document.fromString("alloy#simpleRestJson"), "params" -> smithy4s.Document.obj("value" -> smithy4s.Document.fromDouble(-1.0d), "message" -> smithy4s.Document.fromString("some error message"), "details" -> smithy4s.Document.obj("date" -> smithy4s.Document.fromDouble(123.0d), "location" -> smithy4s.Document.fromString("NYC"))), "code" -> smithy4s.Document.fromDouble(504.0d), "body" -> smithy4s.Document.fromString("{\"value\":-1,\"message\":\"some error message\",\"details\":{\"date\":123,\"location\":\"NYC\"}}"), "bodyMediaType" -> smithy4s.Document.fromString("application/json"), "requireHeaders" -> smithy4s.Document.array(smithy4s.Document.fromString("X-Error-Type"))), smithy4s.Document.obj("id" -> smithy4s.Document.fromString("complex_error_no_details"), "protocol" -> smithy4s.Document.fromString("alloy#simpleRestJson"), "params" -> smithy4s.Document.obj("value" -> smithy4s.Document.fromDouble(-1.0d), "message" -> smithy4s.Document.fromString("some error message")), "code" -> smithy4s.Document.fromDouble(504.0d), "body" -> smithy4s.Document.fromString("{\"value\":-1,\"message\":\"some error message\"}"), "bodyMediaType" -> smithy4s.Document.fromString("application/json"), "requireHeaders" -> smithy4s.Document.array(smithy4s.Document.fromString("X-Error-Type"))))),
  ).lazily

  // constructor using the original order from the spec
  private def make(value: Int, message: String, details: Option[ErrorDetails]): ComplexError = ComplexError(value, message, details)

  implicit val schema: Schema[ComplexError] = struct(
    int.required[ComplexError]("value", _.value),
    string.required[ComplexError]("message", _.message),
    ErrorDetails.schema.optional[ComplexError]("details", _.details),
  )(make).withId(id).addHints(hints)
}
