package com.amazonaws.dynamodb

import smithy.api.Documentation
import smithy.api.Error
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** <p>An error occurred on the server side.</p>
  * @param message
  *   <p>The server encountered an internal error trying to fulfill the request.</p>
  */
final case class InternalServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object InternalServerError extends ShapeTag.Companion[InternalServerError] {

  val message: FieldLens[InternalServerError, Option[String]] = string.optional[InternalServerError]("message", _.message, n => c => c.copy(message = n)).addHints(Documentation("<p>The server encountered an internal error trying to fulfill the request.</p>"))

  implicit val schema: Schema[InternalServerError] = struct(
    message,
  ){
    InternalServerError.apply
  }
  .withId(ShapeId("com.amazonaws.dynamodb", "InternalServerError"))
  .addHints(
    Documentation("<p>An error occurred on the server side.</p>"),
    Error.SERVER.widen,
  )
}
