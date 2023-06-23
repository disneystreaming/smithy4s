package smithy4s.example.product

import smithy4s.Endpoint
import smithy4s.Errorable
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ServiceProduct
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.example.BucketName
import smithy4s.example.LowHigh
import smithy4s.example.NoMoreSpace
import smithy4s.example.ObjectKey
import smithy4s.example.PutObjectInput
import smithy4s.example.SomeValue
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.UnionSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

trait ObjectServiceGen[F[_, _, _, _, _]] {
  self =>

  def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None): F[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ObjectServiceGen[F]] = Transformation.of[ObjectServiceGen[F]](this)
}

trait ObjectServiceProductGen[F[_, _, _, _, _]] {
  self =>

  def putObject: F[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing]
}

object ObjectServiceGen extends Service.Mixin[ObjectServiceGen, ObjectServiceOperation] with ServiceProduct.Mirror[ObjectServiceGen] {

  val id: ShapeId = ShapeId("smithy4s.example.product", "ObjectService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: List[smithy4s.Endpoint[ObjectServiceOperation, _, _, _, _, _]] = List(
    ObjectServiceOperation.PutObject,
  )

  def endpoint[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ObjectServiceOperation.Transformed[ObjectServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ObjectServiceGen[ObjectServiceOperation] = ObjectServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]): ObjectServiceGen[P1] = new ObjectServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ObjectServiceOperation, P]): ObjectServiceGen[P] = new ObjectServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectServiceGen[P]): PolyFunction5[ObjectServiceOperation, P] = ObjectServiceOperation.toPolyFunction(impl)

  type PutObjectError = ObjectServiceOperation.PutObjectError
  val PutObjectError = ObjectServiceOperation.PutObjectError
  type Prod[F[_, _, _, _, _]] = ObjectServiceProductGen[F]
  val serviceProduct: ServiceProduct.Aux[ObjectServiceProductGen, ObjectServiceGen] = ObjectServiceProductGen
}

object ObjectServiceProductGen extends ServiceProduct[ObjectServiceProductGen] {
  type Alg[F[_, _, _, _, _]] = ObjectServiceGen[F]
  val service: ObjectServiceGen.type = ObjectServiceGen

  def endpointsProduct: ObjectServiceProductGen[service.Endpoint] = new ObjectServiceProductGen[service.Endpoint] {
    def putObject: service.Endpoint[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = ObjectServiceOperation.PutObject
  }

  def toPolyFunction[P2[_, _, _, _, _]](algebra: ObjectServiceProductGen[P2]) = new PolyFunction5[service.Endpoint, P2] {
    def apply[I, E, O, SI, SO](fa: service.Endpoint[I, E, O, SI, SO]): P2[I, E, O, SI, SO] =
    fa match {
      case ObjectServiceOperation.PutObject => algebra.putObject.asInstanceOf[P2[I, E, O, SI, SO]]
    }
  }

  def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: ObjectServiceProductGen[F], f: PolyFunction5[F, G]): ObjectServiceProductGen[G] = {
    new ObjectServiceProductGen[G] {
      def putObject: G[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = f[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing](alg.putObject)
    }
  }
}

sealed trait ObjectServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[ObjectServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}

object ObjectServiceOperation {

  object reified extends ObjectServiceGen[ObjectServiceOperation] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = PutObject(PutObjectInput(key, bucketName, data, foo, someValue))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ObjectServiceGen[P], f: PolyFunction5[P, P1]) extends ObjectServiceGen[P1] {
    def putObject(key: ObjectKey, bucketName: BucketName, data: String, foo: Option[LowHigh] = None, someValue: Option[SomeValue] = None) = f[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing](alg.putObject(key, bucketName, data, foo, someValue))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectServiceGen[P]): PolyFunction5[ObjectServiceOperation, P] = new PolyFunction5[ObjectServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ObjectServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class PutObject(input: PutObjectInput) extends ObjectServiceOperation[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectServiceGen[F]): F[PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] = impl.putObject(input.key, input.bucketName, input.data, input.foo, input.someValue)
    def endpoint: (PutObjectInput, smithy4s.Endpoint[ObjectServiceOperation,PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing]) = (input, PutObject)
  }
  object PutObject extends smithy4s.Endpoint[ObjectServiceOperation,PutObjectInput, ObjectServiceOperation.PutObjectError, Unit, Nothing, Nothing] with Errorable[PutObjectError] {
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
      case e: NoMoreSpace => Some(PutObjectError.NoMoreSpaceCase(e))
      case _ => None
    }
    def unliftError(e: PutObjectError): Throwable = e match {
      case PutObjectError.NoMoreSpaceCase(e) => e
    }
  }
  sealed trait PutObjectError extends scala.Product with scala.Serializable {
    @inline final def widen: PutObjectError = this
  }
  object PutObjectError extends ShapeTag.Companion[PutObjectError] {
    val id: ShapeId = ShapeId("smithy4s.example.product", "PutObjectError")

    val hints: Hints = Hints.empty

    final case class NoMoreSpaceCase(noMoreSpace: NoMoreSpace) extends PutObjectError

    object NoMoreSpaceCase {
      val hints: Hints = Hints.empty
      val schema: Schema[NoMoreSpaceCase] = bijection(NoMoreSpace.schema.addHints(hints), NoMoreSpaceCase(_), _.noMoreSpace)
      val alt = schema.oneOf[PutObjectError]("NoMoreSpace")
    }

    implicit val schema: UnionSchema[PutObjectError] = union(
      NoMoreSpaceCase.alt,
    ){
      case c: NoMoreSpaceCase => NoMoreSpaceCase.alt(c)
    }
  }
}

