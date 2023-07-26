package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object TableNameList extends Newtype[List[TableName]] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "TableNameList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[TableName]] = list(TableName.$schema).withId(id).addHints(hints)
  implicit val schema: Schema[TableNameList] = bijection(underlyingSchema, asBijection)
}
