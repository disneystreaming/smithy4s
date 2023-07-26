package com.amazonaws.dynamodb

import smithy.api.Documentation
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

/** <p>Represents the input of a <code>ListTables</code> operation.</p>
  * @param ExclusiveStartTableName
  *   <p>The first table name that this operation will evaluate. Use the value that was returned for
  *           <code>LastEvaluatedTableName</code> in a previous operation, so that you can obtain the next page
  *         of results.</p>
  * @param Limit
  *   <p>A maximum number of table names to return. If this parameter is not specified, the limit is 100.</p>
  */
final case class ListTablesInput(exclusiveStartTableName: Option[TableName] = None, limit: Option[ListTablesInputLimit] = None)
object ListTablesInput extends ShapeTag.Companion[ListTablesInput] {

  val exclusiveStartTableName: FieldLens[ListTablesInput, Option[TableName]] = TableName.schema.optional[ListTablesInput]("ExclusiveStartTableName", _.exclusiveStartTableName, n => c => c.copy(exclusiveStartTableName = n)).addHints(Documentation("<p>The first table name that this operation will evaluate. Use the value that was returned for\n        <code>LastEvaluatedTableName</code> in a previous operation, so that you can obtain the next page\n      of results.</p>"))
  val limit: FieldLens[ListTablesInput, Option[ListTablesInputLimit]] = ListTablesInputLimit.schema.optional[ListTablesInput]("Limit", _.limit, n => c => c.copy(limit = n)).addHints(Documentation("<p>A maximum number of table names to return. If this parameter is not specified, the limit is 100.</p>"))

  implicit val schema: Schema[ListTablesInput] = struct(
    exclusiveStartTableName,
    limit,
  ){
    ListTablesInput.apply
  }
  .withId(ShapeId("com.amazonaws.dynamodb", "ListTablesInput"))
  .addHints(
    Documentation("<p>Represents the input of a <code>ListTables</code> operation.</p>"),
  )
}
