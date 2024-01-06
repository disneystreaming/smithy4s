package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object ListTablesInputLimit extends Newtype[Int] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTablesInputLimit")
  val hints: Hints = Hints(
    smithy.api.Box(),
  )
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints).validated(smithy.api.Range(min = Some(_root_.scala.math.BigDecimal(1.0)), max = Some(_root_.scala.math.BigDecimal(100.0))))
  implicit val schema: Schema[ListTablesInputLimit] = bijection(underlyingSchema, asBijection)
}
