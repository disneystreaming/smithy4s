package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.ErrorSchema
import smithy4s.schema.OperationSchema
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

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
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
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None): PutObject = PutObject(PutObjectInput(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName): GetObject = GetObject(GetObjectInput(key, bucketName))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]) extends ObjectServiceGen[P1] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None): P1[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = f[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing](alg.putObject(key, bucketName, data, foo, someValue))
    def getObject(key: ObjectKey, bucketName: BucketName): P1[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] = f[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing](alg.getObject(key, bucketName))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectServiceGen[P]): PolyFunction5[ObjectServiceOperation, P] = new PolyFunction5[ObjectServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class PutObject(input: PutObjectInput) extends ObjectServiceOperation[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = impl.putObject(input.key, input.bucketName, input.data, input.foo, input.someValue)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ObjectServiceOperation,PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = PutObject
  }
  object PutObject extends smithy4s.Endpoint[ObjectServiceOperation,PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] {
    val schema: OperationSchema[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "PutObject"))
      .withInput(PutObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withError(PutObjectError.errorSchema)
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("PUT"), uri = smithy.api.NonEmptyString("/{bucketName}/{key}"), code = 200), smithy.api.Idempotent())
    def wrap(input: PutObjectInput): PutObject = PutObject(input)
  }
  sealed trait PutObjectError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: PutObjectError = this
    def $ordinal: Int

    object project {
      def serverError: Option[ServerError] = PutObjectError.ServerErrorCase.alt.project.lift(self).map(_.serverError)
      def noMoreSpace: Option[NoMoreSpace] = PutObjectError.NoMoreSpaceCase.alt.project.lift(self).map(_.noMoreSpace)
    }

    def accept[A](visitor: PutObjectError.Visitor[A]): A = this match {
      case value: PutObjectError.ServerErrorCase => visitor.serverError(value.serverError)
      case value: PutObjectError.NoMoreSpaceCase => visitor.noMoreSpace(value.noMoreSpace)
    }
  }
  object PutObjectError extends ErrorSchema.Companion[PutObjectError] {

    def serverError(serverError: ServerError): PutObjectError = ServerErrorCase(serverError)
    def noMoreSpace(noMoreSpace: NoMoreSpace): PutObjectError = NoMoreSpaceCase(noMoreSpace)

    val id: ShapeId = ShapeId("smithy4s.example", "PutObjectError")

    val hints: Hints = Hints.empty

    final case class ServerErrorCase(serverError: ServerError) extends PutObjectError { final def $ordinal: Int = 0 }
    final case class NoMoreSpaceCase(noMoreSpace: NoMoreSpace) extends PutObjectError { final def $ordinal: Int = 1 }

    object ServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[PutObjectError.ServerErrorCase] = bijection(ServerError.schema.addHints(hints), PutObjectError.ServerErrorCase(_), _.serverError)
      val alt = schema.oneOf[PutObjectError]("ServerError")
    }
    object NoMoreSpaceCase {
      val hints: Hints = Hints.empty
      val schema: Schema[PutObjectError.NoMoreSpaceCase] = bijection(NoMoreSpace.schema.addHints(hints), PutObjectError.NoMoreSpaceCase(_), _.noMoreSpace)
      val alt = schema.oneOf[PutObjectError]("NoMoreSpace")
    }

    trait Visitor[A] {
      def serverError(value: ServerError): A
      def noMoreSpace(value: NoMoreSpace): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def serverError(value: ServerError): A = default
        def noMoreSpace(value: NoMoreSpace): A = default
      }
    }

    implicit val schema: Schema[PutObjectError] = union(
      PutObjectError.ServerErrorCase.alt,
      PutObjectError.NoMoreSpaceCase.alt,
    ){
      _.$ordinal
    }
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
  final case class GetObject(input: GetObjectInput) extends ObjectServiceOperation[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] = impl.getObject(input.key, input.bucketName)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[ObjectServiceOperation,GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] = GetObject
  }
  object GetObject extends smithy4s.Endpoint[ObjectServiceOperation,GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetObjectInput, ObjectServiceOperation.GetObjectError, GetObjectOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetObject"))
      .withInput(GetObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withError(GetObjectError.errorSchema)
      .withOutput(GetObjectOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen))
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/{bucketName}/{key}"), code = 200), smithy.api.Readonly())
    def wrap(input: GetObjectInput): GetObject = GetObject(input)
  }
  sealed trait GetObjectError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetObjectError = this
    def $ordinal: Int

    object project {
      def serverError: Option[ServerError] = GetObjectError.ServerErrorCase.alt.project.lift(self).map(_.serverError)
    }

    def accept[A](visitor: GetObjectError.Visitor[A]): A = this match {
      case value: GetObjectError.ServerErrorCase => visitor.serverError(value.serverError)
    }
  }
  object GetObjectError extends ErrorSchema.Companion[GetObjectError] {

    def serverError(serverError: ServerError): GetObjectError = ServerErrorCase(serverError)

    val id: ShapeId = ShapeId("smithy4s.example", "GetObjectError")

    val hints: Hints = Hints.empty

    final case class ServerErrorCase(serverError: ServerError) extends GetObjectError { final def $ordinal: Int = 0 }

    object ServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetObjectError.ServerErrorCase] = bijection(ServerError.schema.addHints(hints), GetObjectError.ServerErrorCase(_), _.serverError)
      val alt = schema.oneOf[GetObjectError]("ServerError")
    }

    trait Visitor[A] {
      def serverError(value: ServerError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def serverError(value: ServerError): A = default
      }
    }

    implicit val schema: Schema[GetObjectError] = union(
      GetObjectError.ServerErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetObjectError] = throwable match {
      case e: ServerError => Some(GetObjectError.ServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetObjectError): Throwable = e match {
      case GetObjectError.ServerErrorCase(e) => e
    }
  }
}

