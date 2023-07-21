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


}
