package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** <p>Represents the output of a <code>ListTables</code> operation.</p>
  * @param TableNames
  *   <p>The names of the tables associated with the current account at the current endpoint. The maximum size of this array is 100.</p>
  *            <p>If <code>LastEvaluatedTableName</code> also appears in the output, you can use this value as the
  *           <code>ExclusiveStartTableName</code> parameter in a subsequent <code>ListTables</code> request and
  *         obtain the next page of results.</p>
  * @param LastEvaluatedTableName
  *   <p>The name of the last table in the current page of results. Use this value as the
  *           <code>ExclusiveStartTableName</code> in a new request to obtain the next page of results, until
  *         all the table names are returned.</p>
  *            <p>If you do not receive a <code>LastEvaluatedTableName</code> value in the response, this means that
  *         there are no more table names to be retrieved.</p>
  */
final case class ListTablesOutput(tableNames: Option[List[TableName]] = None, lastEvaluatedTableName: Option[TableName] = None)
object ListTablesOutput extends ShapeTag.Companion[ListTablesOutput] {
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTablesOutput")

  val hints: Hints = Hints(
    smithy.api.Documentation("<p>Represents the output of a <code>ListTables</code> operation.</p>"),
  )

  val tableNames = TableNameList.underlyingSchema.optional[ListTablesOutput]("TableNames", _.tableNames).addHints(smithy.api.Documentation("<p>The names of the tables associated with the current account at the current endpoint. The maximum size of this array is 100.</p>\n         <p>If <code>LastEvaluatedTableName</code> also appears in the output, you can use this value as the\n        <code>ExclusiveStartTableName</code> parameter in a subsequent <code>ListTables</code> request and\n      obtain the next page of results.</p>"))
  val lastEvaluatedTableName = TableName.schema.optional[ListTablesOutput]("LastEvaluatedTableName", _.lastEvaluatedTableName).addHints(smithy.api.Documentation("<p>The name of the last table in the current page of results. Use this value as the\n        <code>ExclusiveStartTableName</code> in a new request to obtain the next page of results, until\n      all the table names are returned.</p>\n         <p>If you do not receive a <code>LastEvaluatedTableName</code> value in the response, this means that\n      there are no more table names to be retrieved.</p>"))

  implicit val schema: Schema[ListTablesOutput] = struct(
    tableNames,
    lastEvaluatedTableName,
  ){
    ListTablesOutput.apply
  }.withId(id).addHints(hints)
}
