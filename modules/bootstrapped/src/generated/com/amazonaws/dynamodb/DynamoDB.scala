package com.amazonaws.dynamodb

import smithy4s.Errorable
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.UnionSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

/** <fullname>Amazon DynamoDB</fullname>
  * 
  * 
  *          <p>Amazon DynamoDB is a fully managed NoSQL database service that provides fast and
  *       predictable performance with seamless scalability. DynamoDB lets you offload the
  *       administrative burdens of operating and scaling a distributed database, so that you don't have
  *       to worry about hardware provisioning, setup and configuration, replication, software patching,
  *       or cluster scaling.</p>
  * 
  *          <p>With DynamoDB, you can create database tables that can store and retrieve any amount of
  *       data, and serve any level of request traffic. You can scale up or scale down your tables'
  *       throughput capacity without downtime or performance degradation, and use the AWS Management
  *       Console to monitor resource utilization and performance metrics.</p>
  * 
  *          <p>DynamoDB automatically spreads the data and traffic for your tables over a sufficient
  *       number of servers to handle your throughput and storage requirements, while maintaining
  *       consistent and fast performance. All of your data is stored on solid state disks (SSDs) and
  *       automatically replicated across multiple Availability Zones in an AWS region, providing
  *       built-in high availability and data durability. </p>
  */
trait DynamoDBGen[F[_, _, _, _, _]] {
  self =>

  /** <p>Returns the regional endpoint information.</p> */
  def describeEndpoints(): F[DescribeEndpointsRequest, Nothing, DescribeEndpointsResponse, Nothing, Nothing]
  /** <p>Returns an array of table names associated with the current account and endpoint. The output
    *       from <code>ListTables</code> is paginated, with each page returning a maximum of 100 table
    *       names.</p>
    * @param ExclusiveStartTableName
    *   <p>The first table name that this operation will evaluate. Use the value that was returned for
    *           <code>LastEvaluatedTableName</code> in a previous operation, so that you can obtain the next page
    *         of results.</p>
    * @param Limit
    *   <p>A maximum number of table names to return. If this parameter is not specified, the limit is 100.</p>
    */
  def listTables(exclusiveStartTableName: Option[TableName] = None, limit: Option[ListTablesInputLimit] = None): F[ListTablesInput, DynamoDBOperation.ListTablesError, ListTablesOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[DynamoDBGen[F]] = Transformation.of[DynamoDBGen[F]](this)
}

object DynamoDBGen extends Service.Mixin[DynamoDBGen, DynamoDBOperation] {

  val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "DynamoDB_20120810")
  val version: String = "2012-08-10"

  val hints: Hints = Hints(
    aws.auth.Sigv4(name = "dynamodb"),
    smithy.api.Title("Amazon DynamoDB"),
    aws.protocols.AwsJson1_0(http = None, eventStreamHttp = None),
    smithy.api.Documentation("<fullname>Amazon DynamoDB</fullname>\n\n\n         <p>Amazon DynamoDB is a fully managed NoSQL database service that provides fast and\n      predictable performance with seamless scalability. DynamoDB lets you offload the\n      administrative burdens of operating and scaling a distributed database, so that you don\'t have\n      to worry about hardware provisioning, setup and configuration, replication, software patching,\n      or cluster scaling.</p>\n\n         <p>With DynamoDB, you can create database tables that can store and retrieve any amount of\n      data, and serve any level of request traffic. You can scale up or scale down your tables\'\n      throughput capacity without downtime or performance degradation, and use the AWS Management\n      Console to monitor resource utilization and performance metrics.</p>\n\n         <p>DynamoDB automatically spreads the data and traffic for your tables over a sufficient\n      number of servers to handle your throughput and storage requirements, while maintaining\n      consistent and fast performance. All of your data is stored on solid state disks (SSDs) and\n      automatically replicated across multiple Availability Zones in an AWS region, providing\n      built-in high availability and data durability. </p>"),
    aws.api.Service(sdkId = "DynamoDB", arnNamespace = Some(aws.api.ArnNamespace("dynamodb")), cloudFormationName = Some(aws.api.CloudFormationName("DynamoDB")), cloudTrailEventSource = Some("dynamodb.amazonaws.com"), endpointPrefix = Some("dynamodb")),
    smithy.api.XmlNamespace(uri = smithy.api.NonEmptyString("http://dynamodb.amazonaws.com/doc/2012-08-10/"), prefix = None),
    aws.api.ClientEndpointDiscovery(operation = "com.amazonaws.dynamodb#DescribeEndpoints", error = Some("com.amazonaws.dynamodb#InvalidEndpointException")),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[DynamoDBOperation, _, _, _, _, _]] = Vector(
    DynamoDBOperation.DescribeEndpoints,
    DynamoDBOperation.ListTables,
  )

  def input[I, E, O, SI, SO](op: DynamoDBOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: DynamoDBOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: DynamoDBOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends DynamoDBOperation.Transformed[DynamoDBOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: DynamoDBGen[DynamoDBOperation] = DynamoDBOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: DynamoDBGen[P], f: PolyFunction5[P, P1]): DynamoDBGen[P1] = new DynamoDBOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[DynamoDBOperation, P]): DynamoDBGen[P] = new DynamoDBOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: DynamoDBGen[P]): PolyFunction5[DynamoDBOperation, P] = DynamoDBOperation.toPolyFunction(impl)

  type ListTablesError = DynamoDBOperation.ListTablesError
  val ListTablesError = DynamoDBOperation.ListTablesError
}

sealed trait DynamoDBOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: DynamoDBGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: smithy4s.Endpoint[DynamoDBOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object DynamoDBOperation {

  object reified extends DynamoDBGen[DynamoDBOperation] {
    def describeEndpoints() = DescribeEndpoints(DescribeEndpointsRequest())
    def listTables(exclusiveStartTableName: Option[TableName] = None, limit: Option[ListTablesInputLimit] = None) = ListTables(ListTablesInput(exclusiveStartTableName, limit))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: DynamoDBGen[P], f: PolyFunction5[P, P1]) extends DynamoDBGen[P1] {
    def describeEndpoints() = f[DescribeEndpointsRequest, Nothing, DescribeEndpointsResponse, Nothing, Nothing](alg.describeEndpoints())
    def listTables(exclusiveStartTableName: Option[TableName] = None, limit: Option[ListTablesInputLimit] = None) = f[ListTablesInput, DynamoDBOperation.ListTablesError, ListTablesOutput, Nothing, Nothing](alg.listTables(exclusiveStartTableName, limit))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: DynamoDBGen[P]): PolyFunction5[DynamoDBOperation, P] = new PolyFunction5[DynamoDBOperation, P] {
    def apply[I, E, O, SI, SO](op: DynamoDBOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DescribeEndpoints(input: DescribeEndpointsRequest) extends DynamoDBOperation[DescribeEndpointsRequest, Nothing, DescribeEndpointsResponse, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DynamoDBGen[F]): F[DescribeEndpointsRequest, Nothing, DescribeEndpointsResponse, Nothing, Nothing] = impl.describeEndpoints()
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[DynamoDBOperation,DescribeEndpointsRequest, Nothing, DescribeEndpointsResponse, Nothing, Nothing] = DescribeEndpoints
  }
  object DescribeEndpoints extends smithy4s.Endpoint[DynamoDBOperation,DescribeEndpointsRequest, Nothing, DescribeEndpointsResponse, Nothing, Nothing] {
    val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "DescribeEndpoints")
    val input: Schema[DescribeEndpointsRequest] = DescribeEndpointsRequest.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[DescribeEndpointsResponse] = DescribeEndpointsResponse.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Documentation("<p>Returns the regional endpoint information.</p>"),
    )
    def wrap(input: DescribeEndpointsRequest) = DescribeEndpoints(input)
    override val errorable: Option[Nothing] = None
  }
  final case class ListTables(input: ListTablesInput) extends DynamoDBOperation[ListTablesInput, DynamoDBOperation.ListTablesError, ListTablesOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DynamoDBGen[F]): F[ListTablesInput, DynamoDBOperation.ListTablesError, ListTablesOutput, Nothing, Nothing] = impl.listTables(input.exclusiveStartTableName, input.limit)
    def ordinal = 1
    def endpoint: smithy4s.Endpoint[DynamoDBOperation,ListTablesInput, DynamoDBOperation.ListTablesError, ListTablesOutput, Nothing, Nothing] = ListTables
  }
  object ListTables extends smithy4s.Endpoint[DynamoDBOperation,ListTablesInput, DynamoDBOperation.ListTablesError, ListTablesOutput, Nothing, Nothing] with Errorable[ListTablesError] {
    val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTables")
    val input: Schema[ListTablesInput] = ListTablesInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[ListTablesOutput] = ListTablesOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      aws.api.ClientDiscoveredEndpoint(required = false),
      smithy.api.Documentation("<p>Returns an array of table names associated with the current account and endpoint. The output\n      from <code>ListTables</code> is paginated, with each page returning a maximum of 100 table\n      names.</p>"),
      smithy.api.Paginated(inputToken = Some(smithy.api.NonEmptyString("ExclusiveStartTableName")), outputToken = Some(smithy.api.NonEmptyString("LastEvaluatedTableName")), items = Some(smithy.api.NonEmptyString("TableNames")), pageSize = Some(smithy.api.NonEmptyString("Limit"))),
    )
    def wrap(input: ListTablesInput) = ListTables(input)
    override val errorable: Option[Errorable[ListTablesError]] = Some(this)
    val error: UnionSchema[ListTablesError] = ListTablesError.schema
    def liftError(throwable: Throwable): Option[ListTablesError] = throwable match {
      case e: InternalServerError => Some(ListTablesError.InternalServerErrorCase(e))
      case e: InvalidEndpointException => Some(ListTablesError.InvalidEndpointExceptionCase(e))
      case _ => None
    }
    def unliftError(e: ListTablesError): Throwable = e match {
      case ListTablesError.InternalServerErrorCase(e) => e
      case ListTablesError.InvalidEndpointExceptionCase(e) => e
    }
  }
  sealed trait ListTablesError extends scala.Product with scala.Serializable {
    @inline final def widen: ListTablesError = this
    def _ordinal: Int
  }
  object ListTablesError extends ShapeTag.Companion[ListTablesError] {

    def internalServerError(internalServerError:InternalServerError): ListTablesError = InternalServerErrorCase(internalServerError)
    def invalidEndpointException(invalidEndpointException:InvalidEndpointException): ListTablesError = InvalidEndpointExceptionCase(invalidEndpointException)

    val id: ShapeId = ShapeId("com.amazonaws.dynamodb", "ListTablesError")

    val hints: Hints = Hints.empty

    final case class InternalServerErrorCase(internalServerError: InternalServerError) extends ListTablesError { final def _ordinal: Int = 0 }
    final case class InvalidEndpointExceptionCase(invalidEndpointException: InvalidEndpointException) extends ListTablesError { final def _ordinal: Int = 1 }

    object InternalServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[InternalServerErrorCase] = bijection(InternalServerError.schema.addHints(hints), InternalServerErrorCase(_), _.internalServerError)
      val alt = schema.oneOf[ListTablesError]("InternalServerError")
    }
    object InvalidEndpointExceptionCase {
      val hints: Hints = Hints.empty
      val schema: Schema[InvalidEndpointExceptionCase] = bijection(InvalidEndpointException.schema.addHints(hints), InvalidEndpointExceptionCase(_), _.invalidEndpointException)
      val alt = schema.oneOf[ListTablesError]("InvalidEndpointException")
    }

    implicit val schema: UnionSchema[ListTablesError] = union(
      InternalServerErrorCase.alt,
      InvalidEndpointExceptionCase.alt,
    ){
      _._ordinal
    }
  }
}

