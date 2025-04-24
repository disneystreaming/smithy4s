package com.amazonaws

package object dynamodb {
  type DynamoDB[F[_]] = smithy4s.kinds.FunctorAlgebra[DynamoDBGen, F]
  val DynamoDB = DynamoDBGen

  /** @param member
    *   <p>An endpoint information details.</p>
    */
  type Endpoints = com.amazonaws.dynamodb.Endpoints.Type
  type ErrorMessage = com.amazonaws.dynamodb.ErrorMessage.Type
  type ListTablesInputLimit = com.amazonaws.dynamodb.ListTablesInputLimit.Type
  type TableName = com.amazonaws.dynamodb.TableName.Type
  type TableNameList = com.amazonaws.dynamodb.TableNameList.Type

}