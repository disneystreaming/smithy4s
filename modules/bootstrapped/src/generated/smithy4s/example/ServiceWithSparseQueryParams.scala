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

trait ServiceWithSparseQueryParamsGen[F[_, _, _, _, _]] {
  self =>

  def getOperation(foo: List[Option[String]]): F[SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ServiceWithSparseQueryParamsGen[F]] = Transformation.of[ServiceWithSparseQueryParamsGen[F]](this)
}

object ServiceWithSparseQueryParamsGen extends Service.Mixin[ServiceWithSparseQueryParamsGen, ServiceWithSparseQueryParamsOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ServiceWithSparseQueryParams")
  val version: String = "1.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ServiceWithSparseQueryParamsOperation, _, _, _, _, _]] = Vector(
    ServiceWithSparseQueryParamsOperation.GetOperation,
  )

  def input[I, E, O, SI, SO](op: ServiceWithSparseQueryParamsOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ServiceWithSparseQueryParamsOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ServiceWithSparseQueryParamsOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ServiceWithSparseQueryParamsOperation.Transformed[ServiceWithSparseQueryParamsOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ServiceWithSparseQueryParamsGen[ServiceWithSparseQueryParamsOperation] = ServiceWithSparseQueryParamsOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ServiceWithSparseQueryParamsGen[P], f: PolyFunction5[P, P1]): ServiceWithSparseQueryParamsGen[P1] = new ServiceWithSparseQueryParamsOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ServiceWithSparseQueryParamsOperation, P]): ServiceWithSparseQueryParamsGen[P] = new ServiceWithSparseQueryParamsOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ServiceWithSparseQueryParamsGen[P]): PolyFunction5[ServiceWithSparseQueryParamsOperation, P] = ServiceWithSparseQueryParamsOperation.toPolyFunction(impl)

}

sealed trait ServiceWithSparseQueryParamsOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ServiceWithSparseQueryParamsGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ServiceWithSparseQueryParamsOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ServiceWithSparseQueryParamsOperation {

  object reified extends ServiceWithSparseQueryParamsGen[ServiceWithSparseQueryParamsOperation] {
    def getOperation(foo: List[Option[String]]): GetOperation = GetOperation(SparseQueryInput(foo))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ServiceWithSparseQueryParamsGen[P], f: PolyFunction5[P, P1]) extends ServiceWithSparseQueryParamsGen[P1] {
    def getOperation(foo: List[Option[String]]): P1[SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing] = f[SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing](alg.getOperation(foo))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ServiceWithSparseQueryParamsGen[P]): PolyFunction5[ServiceWithSparseQueryParamsOperation, P] = new PolyFunction5[ServiceWithSparseQueryParamsOperation, P] {
    def apply[I, E, O, SI, SO](op: ServiceWithSparseQueryParamsOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetOperation(input: SparseQueryInput) extends ServiceWithSparseQueryParamsOperation[SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ServiceWithSparseQueryParamsGen[F]): F[SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing] = impl.getOperation(input.foo)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ServiceWithSparseQueryParamsOperation,SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing] = GetOperation
  }
  object GetOperation extends smithy4s.Endpoint[ServiceWithSparseQueryParamsOperation,SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[SparseQueryInput, Nothing, SparseQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetOperation"))
      .withInput(SparseQueryInput.schema)
      .withOutput(SparseQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/operation/sparse-query-params"), code = 200))
    def wrap(input: SparseQueryInput): GetOperation = GetOperation(input)
  }
}

