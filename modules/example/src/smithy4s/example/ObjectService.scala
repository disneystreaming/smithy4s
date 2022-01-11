package smithy4s.example

import ObjectServiceGen.GetObjectError
import ObjectServiceGen.PutObjectError
import smithy4s.http
import smithy4s.syntax._

trait ObjectServiceGen[F[_, _, _, _, _]] {
  self =>

  def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) : F[PutObjectInput, PutObjectError, Unit, Nothing, Nothing]
  def getObject(key: ObjectKey, bucketName: BucketName) : F[GetObjectInput, GetObjectError, GetObjectOutput, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) : ObjectServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) extends ObjectServiceGen[G] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = transformation[PutObjectInput, PutObjectError, Unit, Nothing, Nothing](self.putObject(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName) = transformation[GetObjectInput, GetObjectError, GetObjectOutput, Nothing, Nothing](self.getObject(key, bucketName))
  }
}

object ObjectServiceGen extends smithy4s.Service[ObjectServiceGen, ObjectServiceOperation] {

  def apply[F[_]](implicit F: smithy4s.Monadic[ObjectServiceGen, F]): F.type = F

  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ObjectService")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
    smithy4s.api.SimpleRestJson(),
  )

  val endpoints = List(
    PutObject,
    GetObject,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : ObjectServiceOperation[I, E, O, SI, SO]) = op match {
    case PutObject(input) => (input, PutObject)
    case GetObject(input) => (input, GetObject)
  }

  object reified extends ObjectServiceGen[ObjectServiceOperation] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = PutObject(PutObjectInput(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName) = GetObject(GetObjectInput(key, bucketName))
  }

  def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[ObjectServiceOperation, P]): ObjectServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ObjectServiceGen[P], transformation: smithy4s.Transformation[P, P1]): ObjectServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : ObjectServiceGen[P]): smithy4s.Transformation[ObjectServiceOperation, P] = new smithy4s.Transformation[ObjectServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ObjectServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case PutObject(PutObjectInput(key, bucketName, data, foo, someValue)) => impl.putObject(key, bucketName, data, foo, someValue)
      case GetObject(GetObjectInput(key, bucketName)) => impl.getObject(key, bucketName)
    }
  }
  case class PutObject(input: PutObjectInput) extends ObjectServiceOperation[PutObjectInput, PutObjectError, Unit, Nothing, Nothing]
  object PutObject extends smithy4s.Endpoint[ObjectServiceOperation, PutObjectInput, PutObjectError, Unit, Nothing, Nothing] with http.HttpEndpoint[PutObjectInput] with smithy4s.Errorable[PutObjectError] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PutObject")
    val input: smithy4s.Schema[PutObjectInput] = PutObjectInput.schema.withHints(smithy4s.internals.InputOutput.Input)
    val output: smithy4s.Schema[Unit] = unit.withHints(smithy4s.internals.InputOutput.Output)
    val streamedInput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val streamedOutput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
      smithy.api.Idempotent(),
      smithy.api.Http(smithy.api.NonEmptyString("PUT"), smithy.api.NonEmptyString("/{bucketName}/{key}"), Some(200)),
    )
    def wrap(input: PutObjectInput) = PutObject(input)
    override val errorable: Option[smithy4s.Errorable[PutObjectError]] = Some(this)
    val error: smithy4s.errorUnion.Schema[PutObjectError] = PutObjectError.schema
    def liftError(throwable: Throwable) : Option[PutObjectError] = throwable match {
      case e: ServerError => Some(PutObjectError.ServerErrorCase(e))
      case e: NoMoreSpace => Some(PutObjectError.NoMoreSpaceCase(e))
      case _ => None
    }
    def unliftError(e: PutObjectError) : Throwable = e match {
      case PutObjectError.ServerErrorCase(e) => e
      case PutObjectError.NoMoreSpaceCase(e) => e
    }
    def path(input: PutObjectInput) = s"${smithy4s.segment(input.bucketName)}/${smithy4s.segment(input.key)}"
    val path = List(http.PathSegment.label("bucketName"), http.PathSegment.label("key"))
    val code: Int = 200
    val method: http.HttpMethod = http.HttpMethod.PUT
  }
  sealed trait PutObjectError
  object PutObjectError {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PutObjectError")

    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
    )

    case class ServerErrorCase(serverError: ServerError) extends PutObjectError
    case class NoMoreSpaceCase(noMoreSpace: NoMoreSpace) extends PutObjectError

    object ServerErrorCase {
      val hints : smithy4s.Hints = smithy4s.Hints()
      val schema: smithy4s.Schema[ServerErrorCase] = bijection(ServerError.schema, ServerErrorCase(_), _.serverError)
      val alt = schema.oneOf[PutObjectError]("ServerError")
    }
    object NoMoreSpaceCase {
      val hints : smithy4s.Hints = smithy4s.Hints()
      val schema: smithy4s.Schema[NoMoreSpaceCase] = bijection(NoMoreSpace.schema, NoMoreSpaceCase(_), _.noMoreSpace)
      val alt = schema.oneOf[PutObjectError]("NoMoreSpace")
    }

    val schema: smithy4s.errorUnion.Schema[PutObjectError] = errors(
      ServerErrorCase.alt,
      NoMoreSpaceCase.alt,
    ){
      case c : ServerErrorCase => ServerErrorCase.alt(c)
      case c : NoMoreSpaceCase => NoMoreSpaceCase.alt(c)
    }
    implicit val staticSchema : schematic.Static[smithy4s.Schema[PutObjectError]] = schematic.Static(schema)
  }
  case class GetObject(input: GetObjectInput) extends ObjectServiceOperation[GetObjectInput, GetObjectError, GetObjectOutput, Nothing, Nothing]
  object GetObject extends smithy4s.Endpoint[ObjectServiceOperation, GetObjectInput, GetObjectError, GetObjectOutput, Nothing, Nothing] with http.HttpEndpoint[GetObjectInput] with smithy4s.Errorable[GetObjectError] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetObject")
    val input: smithy4s.Schema[GetObjectInput] = GetObjectInput.schema.withHints(smithy4s.internals.InputOutput.Input)
    val output: smithy4s.Schema[GetObjectOutput] = GetObjectOutput.schema.withHints(smithy4s.internals.InputOutput.Output)
    val streamedInput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val streamedOutput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
      smithy.api.Http(smithy.api.NonEmptyString("GET"), smithy.api.NonEmptyString("/{bucketName}/{key}"), Some(200)),
      smithy.api.Readonly(),
    )
    def wrap(input: GetObjectInput) = GetObject(input)
    override val errorable: Option[smithy4s.Errorable[GetObjectError]] = Some(this)
    val error: smithy4s.errorUnion.Schema[GetObjectError] = GetObjectError.schema
    def liftError(throwable: Throwable) : Option[GetObjectError] = throwable match {
      case e: ServerError => Some(GetObjectError.ServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetObjectError) : Throwable = e match {
      case GetObjectError.ServerErrorCase(e) => e
    }
    def path(input: GetObjectInput) = s"${smithy4s.segment(input.bucketName)}/${smithy4s.segment(input.key)}"
    val path = List(http.PathSegment.label("bucketName"), http.PathSegment.label("key"))
    val code: Int = 200
    val method: http.HttpMethod = http.HttpMethod.GET
  }
  sealed trait GetObjectError
  object GetObjectError {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetObjectError")

    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
    )

    case class ServerErrorCase(serverError: ServerError) extends GetObjectError

    object ServerErrorCase {
      val hints : smithy4s.Hints = smithy4s.Hints()
      val schema: smithy4s.Schema[ServerErrorCase] = bijection(ServerError.schema, ServerErrorCase(_), _.serverError)
      val alt = schema.oneOf[GetObjectError]("ServerError")
    }

    val schema: smithy4s.errorUnion.Schema[GetObjectError] = errors(
      ServerErrorCase.alt,
    ){
      case c : ServerErrorCase => ServerErrorCase.alt(c)
    }
    implicit val staticSchema : schematic.Static[smithy4s.Schema[GetObjectError]] = schematic.Static(schema)
  }
}

sealed trait ObjectServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
