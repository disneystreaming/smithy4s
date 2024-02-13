package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.struct

/** <p>An error occurred on the server side.</p>
  * @param message
  *   <p>The server encountered an internal error trying to fulfill the request.</p>
  */
final case class InternalServerError(message: Option[ErrorMessage] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.map(_.value).orNull
}

object InternalServerError extends ShapeTag.Companion[InternalServerError] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "InternalServerError")

  val hints: Hints = Hints(
    smithy.api.Documentation("<p>An error occurred on the server side.</p>"),
    smithy.api.Error.SERVER.widen,
  ).lazily

  implicit val schema: Schema[InternalServerError] = struct(
    ErrorMessage.schema.optional[InternalServerError]("message", _.message).addHints(smithy.api.Documentation("<p>The server encountered an internal error trying to fulfill the request.</p>")),
  ){
    InternalServerError.apply
  }.withId(id).addHints(hints)
}
