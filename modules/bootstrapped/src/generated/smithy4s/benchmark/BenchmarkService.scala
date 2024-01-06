package smithy4s.benchmark

import _root_.smithy4s.Endpoint
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.Service
import _root_.smithy4s.ShapeId
import _root_.smithy4s.Transformation
import _root_.smithy4s.kinds.PolyFunction5
import _root_.smithy4s.kinds.toPolyFunction5.const5
import _root_.smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.unit

trait BenchmarkServiceGen[F[_, _, _, _, _]] {
  self =>

  def createObject(key: String, bucketName: String, payload: S3Object): F[CreateObjectInput, Nothing, Unit, Nothing, Nothing]
  def sendString(key: String, bucketName: String, body: String): F[SendStringInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[BenchmarkServiceGen[F]] = Transformation.of[BenchmarkServiceGen[F]](this)
}

object BenchmarkServiceGen extends Service.Mixin[BenchmarkServiceGen, BenchmarkServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.benchmark", "BenchmarkService")
  val version: String = "1.0.0"

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[BenchmarkServiceOperation, _, _, _, _, _]] = Vector(
    BenchmarkServiceOperation.CreateObject,
    BenchmarkServiceOperation.SendString,
  )

  def input[I, E, O, SI, SO](op: BenchmarkServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: BenchmarkServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: BenchmarkServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends BenchmarkServiceOperation.Transformed[BenchmarkServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: BenchmarkServiceGen[BenchmarkServiceOperation] = BenchmarkServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: BenchmarkServiceGen[P], f: PolyFunction5[P, P1]): BenchmarkServiceGen[P1] = new BenchmarkServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[BenchmarkServiceOperation, P]): BenchmarkServiceGen[P] = new BenchmarkServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: BenchmarkServiceGen[P]): PolyFunction5[BenchmarkServiceOperation, P] = BenchmarkServiceOperation.toPolyFunction(impl)

}

sealed trait BenchmarkServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: BenchmarkServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[BenchmarkServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object BenchmarkServiceOperation {

  object reified extends BenchmarkServiceGen[BenchmarkServiceOperation] {
    def createObject(key: String, bucketName: String, payload: S3Object): CreateObject = CreateObject(CreateObjectInput(key, bucketName, payload))
    def sendString(key: String, bucketName: String, body: String): SendString = SendString(SendStringInput(key, bucketName, body))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: BenchmarkServiceGen[P], f: PolyFunction5[P, P1]) extends BenchmarkServiceGen[P1] {
    def createObject(key: String, bucketName: String, payload: S3Object): P1[CreateObjectInput, Nothing, Unit, Nothing, Nothing] = f[CreateObjectInput, Nothing, Unit, Nothing, Nothing](alg.createObject(key, bucketName, payload))
    def sendString(key: String, bucketName: String, body: String): P1[SendStringInput, Nothing, Unit, Nothing, Nothing] = f[SendStringInput, Nothing, Unit, Nothing, Nothing](alg.sendString(key, bucketName, body))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: BenchmarkServiceGen[P]): PolyFunction5[BenchmarkServiceOperation, P] = new PolyFunction5[BenchmarkServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: BenchmarkServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class CreateObject(input: CreateObjectInput) extends BenchmarkServiceOperation[CreateObjectInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: BenchmarkServiceGen[F]): F[CreateObjectInput, Nothing, Unit, Nothing, Nothing] = impl.createObject(input.key, input.bucketName, input.payload)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[BenchmarkServiceOperation,CreateObjectInput, Nothing, Unit, Nothing, Nothing] = CreateObject
  }
  object CreateObject extends smithy4s.Endpoint[BenchmarkServiceOperation,CreateObjectInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[CreateObjectInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.benchmark", "CreateObject"))
      .withInput(CreateObjectInput.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/complex/{bucketName}/{key}"), code = 200))
    def wrap(input: CreateObjectInput): CreateObject = CreateObject(input)
  }
  final case class SendString(input: SendStringInput) extends BenchmarkServiceOperation[SendStringInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: BenchmarkServiceGen[F]): F[SendStringInput, Nothing, Unit, Nothing, Nothing] = impl.sendString(input.key, input.bucketName, input.body)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[BenchmarkServiceOperation,SendStringInput, Nothing, Unit, Nothing, Nothing] = SendString
  }
  object SendString extends smithy4s.Endpoint[BenchmarkServiceOperation,SendStringInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[SendStringInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.benchmark", "SendString"))
      .withInput(SendStringInput.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/simple/{bucketName}/{key}"), code = 200))
    def wrap(input: SendStringInput): SendString = SendString(input)
  }
}

