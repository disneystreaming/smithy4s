/*
 *  Copyright 2021-2023 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.amazonaws.dynamodb.DescribeEndpointsResponse
import com.amazonaws.dynamodb.Endpoint
import com.amazonaws.dynamodb.ListTablesInput
import com.amazonaws.dynamodb.ListTablesInputLimit
import com.amazonaws.dynamodb.TableName

object Main extends App {

  val listTablesInput = new ListTablesInput(
    // TableName is not flattened because it has
    // @length and @pattern traits applied
    exclusiveStartTableName = Some(TableName("example-table-name")),

    // ListTablesInputLimit is not flattened because it has
    // a @range constraint
    limit = Some(ListTablesInputLimit(5))
  )

  val describeEndpointsResponse = List(
    Endpoint(
      // Endpoint address was flattened from a com.amazonaws.dynamodb#String
      // to a standard String shape
      address = "example-endpoint",

      // CachePeriodInMinutes was flattened from a com.amazonaws.dynamodb#Long
      // to a standard long
      cachePeriodInMinutes = 5L
    )
  )

  // Ensuring that the AwsConstraintsRemover transformer was applied correctly
  // after the flattening, by removing constraints from any type involved in operation
  // outputs.
  assert(!TableName.hints.has(smithy.api.Pattern))

}
