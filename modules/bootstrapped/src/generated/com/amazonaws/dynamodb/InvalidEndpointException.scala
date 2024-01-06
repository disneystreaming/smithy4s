package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class InvalidEndpointException(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object InvalidEndpointException extends ShapeTag.Companion[InvalidEndpointException] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "InvalidEndpointException")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(421),
  )

  implicit val schema: Schema[InvalidEndpointException] = struct(
    string.optional[InvalidEndpointException]("Message", _.message),
  ){
    InvalidEndpointException.apply
  }.withId(id).addHints(hints)
}
