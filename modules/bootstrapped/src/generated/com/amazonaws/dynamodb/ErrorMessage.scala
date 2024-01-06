package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ErrorMessage extends Newtype[String] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ErrorMessage")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[ErrorMessage] = bijection(underlyingSchema, asBijection)
}
