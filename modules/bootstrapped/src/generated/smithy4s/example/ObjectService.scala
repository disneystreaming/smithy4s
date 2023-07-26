package smithy4s.example

import alloy.SimpleRestJson
import smithy.api.Http
import smithy.api.Idempotent
import smithy.api.NonEmptyString
import smithy.api.Readonly
import smithy4s.Bijection
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

  def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None): F[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing]
  /** @param key
    *   Sent in the URI label named "key".
    *   Key can also be seen as the filename
    *   It is always required for a GET operation
    * @param bucketName
    *   Sent in the URI label named "bucketName".
    */
  def getObject(key: ObjectKey, bucketName: BucketName): F[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ObjectServiceGen[F]] = Transformation.of[ObjectServiceGen[F]](this)
}

object ObjectServiceGen extends Service.Mixin[ObjectServiceGen, ObjectServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ObjectService")
  val version: String = "1.0.0"

  val hints: Hints =
  Hints(
    SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ObjectServiceOperation, _, _, _, _, _]] = Vector(
    ObjectServiceOperation.PutObject,
    ObjectServiceOperation.GetObject,
  )

  def input[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ObjectServiceOperation.Transformed[ObjectServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ObjectServiceGen[ObjectServiceOperation] = ObjectServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]): ObjectServiceGen[P1] = new ObjectServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ObjectServiceOperation, P]): ObjectServiceGen[P] = new ObjectServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectServiceGen[P]): PolyFunction5[ObjectServiceOperation, P] = ObjectServiceOperation.toPolyFunction(impl)

  type PutObjectError = ObjectServiceOperation.PutObjectError
  val PutObjectError = ObjectServiceOperation.PutObjectError
  type GetObjectError = ObjectServiceOperation.GetObjectError
  val GetObjectError = ObjectServiceOperation.GetObjectError
}

sealed trait ObjectServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ObjectServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ObjectServiceOperation {

  object reified extends ObjectServiceGen[ObjectServiceOperation] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = PutObject(PutObjectInput(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName) = GetObject(GetObjectInput(key, bucketName))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]) extends ObjectServiceGen[P1] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = f[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing](alg.putObject(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName) = f[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing](alg.getObject(key, bucketName))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectServiceGen[P]): PolyFunction5[ObjectServiceOperation, P] = new PolyFunction5[ObjectServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class PutObject(input: PutObjectInput) extends ObjectServiceOperation[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = impl.putObject(input.key, input.bucketName, input.data, input.foo, input.someValue)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[ObjectServiceOperation,PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = PutObject
  }
  object PutObject extends smithy4s.Endpoint[ObjectServiceOperation,PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] with Errorable[PutObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example", "PutObject")
    val input: Schema[PutObjectInput] = PutObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("PUT"), uri = NonEmptyString("/{bucketName}/{key}"), code = 200),
      Idempotent(),
    )
    def wrap(input: PutObjectInput) = PutObject(input)
    override val errorable: Option[Errorable[PutObjectError]] = Some(this)
    val error: UnionSchema[PutObjectError] = PutObjectError.schema
    def liftError(throwable: Throwable): Option[PutObjectError] = throwable match {
      case e: smithy4s.example.ServerError => Some(PutObjectError.ServerErrorCase(e))
      case e: smithy4s.example.NoMoreSpace => Some(PutObjectError.NoMoreSpaceCase(e))
      case _ => None
    }
    def unliftError(e: PutObjectError): Throwable = e match {
      case PutObjectError.ServerErrorCase(e) => e
      case PutObjectError.NoMoreSpaceCase(e) => e
    }
  }
  sealed trait PutObjectError extends scala.Product with scala.Serializable {
    @inline final def widen: PutObjectError = this
    def _ordinal: Int
  }
  object PutObjectError extends ShapeTag.Companion[PutObjectError] {
    final case class ServerErrorCase(serverError: smithy4s.example.ServerError) extends PutObjectError { final def _ordinal: Int = 0 }
    final case class NoMoreSpaceCase(noMoreSpace: smithy4s.example.NoMoreSpace) extends PutObjectError { final def _ordinal: Int = 1 }

    object ServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.ServerError, ServerErrorCase] = Bijection(ServerErrorCase(_), _.serverError)
      implicit val toValue: Bijection[ServerErrorCase, smithy4s.example.ServerError] = fromValue.swap
      val schema: Schema[ServerErrorCase] = bijection(smithy4s.example.ServerError.schema, fromValue)
    }
    object NoMoreSpaceCase {
      implicit val fromValue: Bijection[smithy4s.example.NoMoreSpace, NoMoreSpaceCase] = Bijection(NoMoreSpaceCase(_), _.noMoreSpace)
      implicit val toValue: Bijection[NoMoreSpaceCase, smithy4s.example.NoMoreSpace] = fromValue.swap
      val schema: Schema[NoMoreSpaceCase] = bijection(smithy4s.example.NoMoreSpace.schema, fromValue)
    }

    val ServerError = ServerErrorCase.schema.oneOf[PutObjectError]("ServerError")
    val NoMoreSpace = NoMoreSpaceCase.schema.oneOf[PutObjectError]("NoMoreSpace")

    implicit val schema: UnionSchema[PutObjectError] = union(
      ServerError,
      NoMoreSpace,
    ){
      _._ordinal
    }
    
  }
  final case class GetObject(input: GetObjectInput) extends ObjectServiceOperation[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] = impl.getObject(input.key, input.bucketName)
    def ordinal = 1
    def endpoint: smithy4s.Endpoint[ObjectServiceOperation,GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] = GetObject
  }
  object GetObject extends smithy4s.Endpoint[ObjectServiceOperation,GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] with Errorable[GetObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetObject")
    val input: Schema[GetObjectInput] = GetObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetObjectOutput] = GetObjectOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/{bucketName}/{key}"), code = 200),
      Readonly(),
    )
    def wrap(input: GetObjectInput) = GetObject(input)
    override val errorable: Option[Errorable[GetObjectError]] = Some(this)
    val error: UnionSchema[GetObjectError] = GetObjectError.schema
    def liftError(throwable: Throwable): Option[GetObjectError] = throwable match {
      case e: smithy4s.example.ServerError => Some(GetObjectError.ServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetObjectError): Throwable = e match {
      case GetObjectError.ServerErrorCase(e) => e
    }
  }
  sealed trait GetObjectError extends scala.Product with scala.Serializable {
    @inline final def widen: GetObjectError = this
    def _ordinal: Int
  }
  object GetObjectError extends ShapeTag.Companion[GetObjectError] {
    final case class ServerErrorCase(serverError: smithy4s.example.ServerError) extends GetObjectError { final def _ordinal: Int = 0 }

    object ServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.ServerError, ServerErrorCase] = Bijection(ServerErrorCase(_), _.serverError)
      implicit val toValue: Bijection[ServerErrorCase, smithy4s.example.ServerError] = fromValue.swap
      val schema: Schema[ServerErrorCase] = bijection(smithy4s.example.ServerError.schema, fromValue)
    }

    val ServerError = ServerErrorCase.schema.oneOf[GetObjectError]("ServerError")

    implicit val schema: UnionSchema[GetObjectError] = union(
      ServerError,
    ){
      _._ordinal
    }
    
  }
}

