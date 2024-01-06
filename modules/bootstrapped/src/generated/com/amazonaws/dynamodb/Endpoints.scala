package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

/** @param member
  *   <p>An endpoint information details.</p>
  */
object Endpoints extends Newtype[List[Endpoint]] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "Endpoints")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Endpoint]] = list(Endpoint.schema).withId(id).addHints(hints)
  implicit val schema: Schema[Endpoints] = bijection(underlyingSchema, asBijection)
}
