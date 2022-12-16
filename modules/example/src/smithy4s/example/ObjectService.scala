package smithy4s.example

import smithy4s.Endpoint
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
import smithy4s.schema.Schema.unit

trait ObjectServiceGen[F[_, _, _, _, _]] {
  self =>

  def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None): F[PutObjectInput, ObjectServiceGen.PutObjectError, Unit, Nothing, Nothing]
  def getObject(key: ObjectKey, bucketName: BucketName): F[GetObjectInput, ObjectServiceGen.GetObjectError, GetObjectOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ObjectServiceGen[F]] = new Transformation.PartiallyApplied[ObjectServiceGen[F]](this)
}

object ObjectServiceGen extends Service.Mixin[ObjectServiceGen, ObjectServiceOperation] {

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val id: ShapeId = ShapeId("smithy4s.example", "ObjectService")

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  val endpoints: List[ObjectServiceGen.Endpoint[_, _, _, _, _]] = List(
    PutObject,
    GetObject,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]) = op.endpoint

  object reified extends ObjectServiceGen[ObjectServiceOperation] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = PutObject(PutObjectInput(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName) = GetObject(GetObjectInput(key, bucketName))
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]): ObjectServiceGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ObjectServiceOperation, P]): ObjectServiceGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]) extends ObjectServiceGen[P1] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = f[PutObjectInput, ObjectServiceGen.PutObjectError, Unit, Nothing, Nothing](alg.putObject(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName) = f[GetObjectInput, ObjectServiceGen.GetObjectError, GetObjectOutput, Nothing, Nothing](alg.getObject(key, bucketName))
  }

  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends Transformed[ObjectServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]

  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectServiceGen[P]): PolyFunction5[ObjectServiceOperation, P] = new PolyFunction5[ObjectServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  case class PutObject(input: PutObjectInput) extends ObjectServiceOperation[PutObjectInput, ObjectServiceGen.PutObjectError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[PutObjectInput, ObjectServiceGen.PutObjectError, Unit, Nothing, Nothing] = impl.putObject(input.key, input.bucketName, input.data, input.foo, input.someValue)
    def endpoint: (PutObjectInput, Endpoint[PutObjectInput, ObjectServiceGen.PutObjectError, Unit, Nothing, Nothing]) = (input, PutObject)
  }
  object PutObject extends ObjectServiceGen.Endpoint[PutObjectInput, ObjectServiceGen.PutObjectError, Unit, Nothing, Nothing] with Errorable[PutObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example", "PutObject")
    val input: Schema[PutObjectInput] = PutObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("PUT"), uri = smithy.api.NonEmptyString("/{bucketName}/{key}"), code = 200),
      smithy.api.Idempotent(),
    )
    def wrap(input: PutObjectInput) = PutObject(input)
    override val errorable: Option[Errorable[PutObjectError]] = Some(this)
    val error: UnionSchema[PutObjectError] = PutObjectError.schema
    def liftError(throwable: Throwable): Option[PutObjectError] = throwable match {
      case e: ServerError => Some(PutObjectError.ServerErrorCase(e))
      case e: NoMoreSpace => Some(PutObjectError.NoMoreSpaceCase(e))
      case _ => None
    }
    def unliftError(e: PutObjectError): Throwable = e match {
      case PutObjectError.ServerErrorCase(e) => e
      case PutObjectError.NoMoreSpaceCase(e) => e
    }
  }
  sealed trait PutObjectError extends scala.Product with scala.Serializable {
    @inline final def widen: PutObjectError = this
  }
  object PutObjectError extends ShapeTag.Companion[PutObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example", "PutObjectError")

    val hints: Hints = Hints.empty

    case class ServerErrorCase(serverError: ServerError) extends PutObjectError
    case class NoMoreSpaceCase(noMoreSpace: NoMoreSpace) extends PutObjectError

    object ServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ServerErrorCase] = bijection(ServerError.schema.addHints(hints), ServerErrorCase(_), _.serverError)
      val alt = schema.oneOf[PutObjectError]("ServerError")
    }
    object NoMoreSpaceCase {
      val hints: Hints = Hints.empty
      val schema: Schema[NoMoreSpaceCase] = bijection(NoMoreSpace.schema.addHints(hints), NoMoreSpaceCase(_), _.noMoreSpace)
      val alt = schema.oneOf[PutObjectError]("NoMoreSpace")
    }

    implicit val schema: UnionSchema[PutObjectError] = union(
      ServerErrorCase.alt,
      NoMoreSpaceCase.alt,
    ){
      case c: ServerErrorCase => ServerErrorCase.alt(c)
      case c: NoMoreSpaceCase => NoMoreSpaceCase.alt(c)
    }
  }
  case class GetObject(input: GetObjectInput) extends ObjectServiceOperation[GetObjectInput, ObjectServiceGen.GetObjectError, GetObjectOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[GetObjectInput, ObjectServiceGen.GetObjectError, GetObjectOutput, Nothing, Nothing] = impl.getObject(input.key, input.bucketName)
    def endpoint: (GetObjectInput, Endpoint[GetObjectInput, ObjectServiceGen.GetObjectError, GetObjectOutput, Nothing, Nothing]) = (input, GetObject)
  }
  object GetObject extends ObjectServiceGen.Endpoint[GetObjectInput, ObjectServiceGen.GetObjectError, GetObjectOutput, Nothing, Nothing] with Errorable[GetObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetObject")
    val input: Schema[GetObjectInput] = GetObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetObjectOutput] = GetObjectOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/{bucketName}/{key}"), code = 200),
      smithy.api.Readonly(),
    )
    def wrap(input: GetObjectInput) = GetObject(input)
    override val errorable: Option[Errorable[GetObjectError]] = Some(this)
    val error: UnionSchema[GetObjectError] = GetObjectError.schema
    def liftError(throwable: Throwable): Option[GetObjectError] = throwable match {
      case e: ServerError => Some(GetObjectError.ServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetObjectError): Throwable = e match {
      case GetObjectError.ServerErrorCase(e) => e
    }
  }
  sealed trait GetObjectError extends scala.Product with scala.Serializable {
    @inline final def widen: GetObjectError = this
  }
  object GetObjectError extends ShapeTag.Companion[GetObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetObjectError")

    val hints: Hints = Hints.empty

    case class ServerErrorCase(serverError: ServerError) extends GetObjectError

    object ServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ServerErrorCase] = bijection(ServerError.schema.addHints(hints), ServerErrorCase(_), _.serverError)
      val alt = schema.oneOf[GetObjectError]("ServerError")
    }

    implicit val schema: UnionSchema[GetObjectError] = union(
      ServerErrorCase.alt,
    ){
      case c: ServerErrorCase => ServerErrorCase.alt(c)
    }
  }
}

sealed trait ObjectServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[ObjectServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}
