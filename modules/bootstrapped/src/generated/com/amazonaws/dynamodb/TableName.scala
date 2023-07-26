package com.amazonaws.dynamodb

import smithy.api.Length
import smithy.api.Pattern
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TableName extends Newtype[String] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "TableName")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(Length(min = Some(3L), max = Some(255L))).validated(Pattern("^[a-zA-Z0-9_.-]+$"))
  implicit val schema: Schema[TableName] = bijection(underlyingSchema, asBijection)
}
