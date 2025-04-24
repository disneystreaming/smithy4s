package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.unit

trait ServiceWithNullsAndDefaultsGen[F[_, _, _, _, _]] {
  self =>

  def defaultNullsOperation(input: DefaultNullsOperationInput): F[DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing]
  def timestampOperation(input: TimestampOperationInput): F[TimestampOperationInput, Nothing, Unit, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[ServiceWithNullsAndDefaultsGen[F]] = Transformation.of[ServiceWithNullsAndDefaultsGen[F]](this)
}

object ServiceWithNullsAndDefaultsGen extends Service.Mixin[ServiceWithNullsAndDefaultsGen, ServiceWithNullsAndDefaultsOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ServiceWithNullsAndDefaults")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation, _, _, _, _, _]] = Vector(
    ServiceWithNullsAndDefaultsOperation.DefaultNullsOperation,
    ServiceWithNullsAndDefaultsOperation.TimestampOperation,
  )

  def input[I, E, O, SI, SO](op: ServiceWithNullsAndDefaultsOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ServiceWithNullsAndDefaultsOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ServiceWithNullsAndDefaultsOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ServiceWithNullsAndDefaultsOperation.Transformed[ServiceWithNullsAndDefaultsOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ServiceWithNullsAndDefaultsGen[ServiceWithNullsAndDefaultsOperation] = ServiceWithNullsAndDefaultsOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ServiceWithNullsAndDefaultsGen[P], f: PolyFunction5[P, P1]): ServiceWithNullsAndDefaultsGen[P1] = new ServiceWithNullsAndDefaultsOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ServiceWithNullsAndDefaultsOperation, P]): ServiceWithNullsAndDefaultsGen[P] = new ServiceWithNullsAndDefaultsOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[P]): PolyFunction5[ServiceWithNullsAndDefaultsOperation, P] = ServiceWithNullsAndDefaultsOperation.toPolyFunction(impl)

}

sealed trait ServiceWithNullsAndDefaultsOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ServiceWithNullsAndDefaultsOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ServiceWithNullsAndDefaultsOperation {

  object reified extends ServiceWithNullsAndDefaultsGen[ServiceWithNullsAndDefaultsOperation] {
    def defaultNullsOperation(input: DefaultNullsOperationInput): DefaultNullsOperation = DefaultNullsOperation(input)
    def timestampOperation(input: TimestampOperationInput): TimestampOperation = TimestampOperation(input)
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ServiceWithNullsAndDefaultsGen[P], f: PolyFunction5[P, P1]) extends ServiceWithNullsAndDefaultsGen[P1] {
    def defaultNullsOperation(input: DefaultNullsOperationInput): P1[DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing] = f[DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing](alg.defaultNullsOperation(input))
    def timestampOperation(input: TimestampOperationInput): P1[TimestampOperationInput, Nothing, Unit, Nothing, Nothing] = f[TimestampOperationInput, Nothing, Unit, Nothing, Nothing](alg.timestampOperation(input))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[P]): PolyFunction5[ServiceWithNullsAndDefaultsOperation, P] = new PolyFunction5[ServiceWithNullsAndDefaultsOperation, P] {
    def apply[I, E, O, SI, SO](op: ServiceWithNullsAndDefaultsOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DefaultNullsOperation(input: DefaultNullsOperationInput) extends ServiceWithNullsAndDefaultsOperation[DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[F]): F[DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing] = impl.defaultNullsOperation(input)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation,DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing] = DefaultNullsOperation
  }
  object DefaultNullsOperation extends smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation,DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing] {
    val schema: OperationSchema[DefaultNullsOperationInput, Nothing, DefaultNullsOperationOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "DefaultNullsOperation"))
      .withInput(DefaultNullsOperationInput.schema)
      .withOutput(DefaultNullsOperationOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/operation/{requiredLabel}"), code = 200))
    def wrap(input: DefaultNullsOperationInput): DefaultNullsOperation = DefaultNullsOperation(input)
  }
  final case class TimestampOperation(input: TimestampOperationInput) extends ServiceWithNullsAndDefaultsOperation[TimestampOperationInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[F]): F[TimestampOperationInput, Nothing, Unit, Nothing, Nothing] = impl.timestampOperation(input)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation,TimestampOperationInput, Nothing, Unit, Nothing, Nothing] = TimestampOperation
  }
  object TimestampOperation extends smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation,TimestampOperationInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[TimestampOperationInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "TimestampOperation"))
      .withInput(TimestampOperationInput.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/timestamp-operation"), code = 200))
    def wrap(input: TimestampOperationInput): TimestampOperation = TimestampOperation(input)
  }
}

