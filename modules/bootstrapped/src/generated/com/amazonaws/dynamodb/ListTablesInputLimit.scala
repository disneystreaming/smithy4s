package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.NewtypeValidated
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object ListTablesInputLimit extends NewtypeValidated[Int] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTablesInputLimit")
  val hints: Hints = Hints(
    smithy.api.Box(),
  ).lazily
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints).validated(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(100.0))))
  implicit val schema: Schema[ListTablesInputLimit] = bijection(underlyingSchema, asBijectionUnsafe)
  val validators: List[Int => Either[String, Int]] = List(
    a => validateInternal(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = Some(scala.math.BigDecimal(100.0))))(a)
  )
  @inline def apply(a: Int): Either[String, ListTablesInputLimit] = validators
    .foldLeft(Right(a): Either[String, Int])((acc, v) => acc.flatMap(v))
    .map(unsafeApply)
}
