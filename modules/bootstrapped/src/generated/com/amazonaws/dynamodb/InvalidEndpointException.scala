package com.amazonaws.dynamodb

import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class InvalidEndpointException(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object InvalidEndpointException extends ShapeTag.$Companion[InvalidEndpointException] {
  val $id: ShapeId = ShapeId("com.amazonaws.dynamodb", "InvalidEndpointException")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
    HttpError(421),
  )

  val message: FieldLens[InvalidEndpointException, Option[String]] = string.optional[InvalidEndpointException]("Message", _.message, n => c => c.copy(message = n))

  implicit val $schema: Schema[InvalidEndpointException] = struct(
    message,
  ){
    InvalidEndpointException.apply
  }.withId($id).addHints($hints)
}
