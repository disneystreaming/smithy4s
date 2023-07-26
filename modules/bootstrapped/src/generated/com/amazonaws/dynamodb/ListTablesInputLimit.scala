package com.amazonaws.dynamodb

import smithy.api.Box
import smithy.api.Range
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object ListTablesInputLimit extends Newtype[Int] {
  val underlyingSchema: Schema[Int] = int
  .withId(ShapeId("com.amazonaws.dynamodb", "ListTablesInputLimit"))
  .addHints(
    Box(),
  )
  .validated(Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(100.0))))
  implicit val schema: Schema[ListTablesInputLimit] = bijection(underlyingSchema, asBijection)
}
