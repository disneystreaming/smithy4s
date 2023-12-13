package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object ListTablesInputLimit extends Newtype[Int] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTablesInputLimit")
  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Box(),
    )
  )
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints).validated(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(100.0))))
  implicit val schema: Schema[ListTablesInputLimit] = bijection(underlyingSchema, asBijection)
}
