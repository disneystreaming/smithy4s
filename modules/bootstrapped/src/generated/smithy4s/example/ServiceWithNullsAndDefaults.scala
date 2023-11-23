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

trait ServiceWithNullsAndDefaultsGen[F[_, _, _, _, _]] {
  self =>

  def operation(input: OperationInput): F[OperationInput, Nothing, OperationOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ServiceWithNullsAndDefaultsGen[F]] = Transformation.of[ServiceWithNullsAndDefaultsGen[F]](this)
}

object ServiceWithNullsAndDefaultsGen extends Service.Mixin[ServiceWithNullsAndDefaultsGen, ServiceWithNullsAndDefaultsOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ServiceWithNullsAndDefaults")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation, _, _, _, _, _]] = Vector(
    ServiceWithNullsAndDefaultsOperation.Operation,
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
    def operation(input: OperationInput): Operation = Operation(input)
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ServiceWithNullsAndDefaultsGen[P], f: PolyFunction5[P, P1]) extends ServiceWithNullsAndDefaultsGen[P1] {
    def operation(input: OperationInput): P1[OperationInput, Nothing, OperationOutput, Nothing, Nothing] = f[OperationInput, Nothing, OperationOutput, Nothing, Nothing](alg.operation(input))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[P]): PolyFunction5[ServiceWithNullsAndDefaultsOperation, P] = new PolyFunction5[ServiceWithNullsAndDefaultsOperation, P] {
    def apply[I, E, O, SI, SO](op: ServiceWithNullsAndDefaultsOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Operation(input: OperationInput) extends ServiceWithNullsAndDefaultsOperation[OperationInput, Nothing, OperationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ServiceWithNullsAndDefaultsGen[F]): F[OperationInput, Nothing, OperationOutput, Nothing, Nothing] = impl.operation(input)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation,OperationInput, Nothing, OperationOutput, Nothing, Nothing] = Operation
  }
  object Operation extends smithy4s.Endpoint[ServiceWithNullsAndDefaultsOperation,OperationInput, Nothing, OperationOutput, Nothing, Nothing] {
    val schema: OperationSchema[OperationInput, Nothing, OperationOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Operation"))
      .withInput(OperationInput.schema)
      .withOutput(OperationOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/operation/{requiredLabel}"), code = 200))
    def wrap(input: OperationInput): Operation = Operation(input)
  }
}

