package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class InvalidEndpointException(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object InvalidEndpointException extends ShapeTag.Companion[InvalidEndpointException] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "InvalidEndpointException")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(421),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): InvalidEndpointException = InvalidEndpointException(message)

  implicit val schema: Schema[InvalidEndpointException] = struct(
    string.optional[InvalidEndpointException]("Message", _.message),
  )(make).withId(id).addHints(hints)
}
