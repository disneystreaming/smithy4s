package com.amazonaws.dynamodb

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
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
  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTablesInput")

  val hints: Hints = Hints(
    smithy.api.Documentation("<p>Represents the input of a <code>ListTables</code> operation.</p>"),
  )

  object Lenses {
    val exclusiveStartTableName = Lens[ListTablesInput, Option[TableName]](_.exclusiveStartTableName)(n => a => a.copy(exclusiveStartTableName = n))
    val limit = Lens[ListTablesInput, Option[ListTablesInputLimit]](_.limit)(n => a => a.copy(limit = n))
  }

  implicit val schema: Schema[ListTablesInput] = struct(
    TableName.schema.optional[ListTablesInput]("ExclusiveStartTableName", _.exclusiveStartTableName).addHints(smithy.api.Documentation("<p>The first table name that this operation will evaluate. Use the value that was returned for\n        <code>LastEvaluatedTableName</code> in a previous operation, so that you can obtain the next page\n      of results.</p>")),
    ListTablesInputLimit.schema.optional[ListTablesInput]("Limit", _.limit).addHints(smithy.api.Documentation("<p>A maximum number of table names to return. If this parameter is not specified, the limit is 100.</p>")),
  ){
    ListTablesInput.apply
  }.withId(id).addHints(hints)
}
