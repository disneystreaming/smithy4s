package com.amazonaws.dynamodb

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object TableNameList extends Newtype[List[TableName]] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "TableNameList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[TableName]] = list(TableName.schema).withId(id).addHints(hints)
  implicit val schema: Schema[TableNameList] = bijection(underlyingSchema, asBijection)
}
