package smithy4s.example.product

import _root_.smithy4s.Endpoint
import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.Service
import _root_.smithy4s.ServiceProduct
import _root_.smithy4s.ShapeId
import _root_.smithy4s.Transformation
import _root_.smithy4s.kinds.PolyFunction5
import _root_.smithy4s.kinds.toPolyFunction5.const5
import _root_.smithy4s.schema.OperationSchema

trait ExampleServiceGen[F[_, _, _, _, _]] {
  self =>

  def exampleOperation(a: String): F[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ExampleServiceGen[F]] = Transformation.of[ExampleServiceGen[F]](this)
}

trait ExampleServiceProductGen[F[_, _, _, _, _]] {
  self =>

  def exampleOperation: F[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing]
}

object ExampleServiceGen extends Service.Mixin[ExampleServiceGen, ExampleServiceOperation] with ServiceProduct.Mirror[ExampleServiceGen] {

  val id: ShapeId = ShapeId("smithy4s.example.product", "ExampleService")
  val version: String = ""

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ExampleServiceOperation, _, _, _, _, _]] = Vector(
    ExampleServiceOperation.ExampleOperation,
  )

  def input[I, E, O, SI, SO](op: ExampleServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ExampleServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ExampleServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ExampleServiceOperation.Transformed[ExampleServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ExampleServiceGen[ExampleServiceOperation] = ExampleServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ExampleServiceGen[P], f: PolyFunction5[P, P1]): ExampleServiceGen[P1] = new ExampleServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ExampleServiceOperation, P]): ExampleServiceGen[P] = new ExampleServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ExampleServiceGen[P]): PolyFunction5[ExampleServiceOperation, P] = ExampleServiceOperation.toPolyFunction(impl)

  type Prod[F[_, _, _, _, _]] = ExampleServiceProductGen[F]
  val serviceProduct: ServiceProduct.Aux[ExampleServiceProductGen, ExampleServiceGen] = ExampleServiceProductGen
}

object ExampleServiceProductGen extends ServiceProduct[ExampleServiceProductGen] {
  type Alg[F[_, _, _, _, _]] = ExampleServiceGen[F]
  val service: ExampleServiceGen.type = ExampleServiceGen

  def endpointsProduct: ExampleServiceProductGen[service.Endpoint] = new ExampleServiceProductGen[service.Endpoint] {
    def exampleOperation: service.Endpoint[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] = ExampleServiceOperation.ExampleOperation
  }

  def toPolyFunction[P2[_, _, _, _, _]](algebra: ExampleServiceProductGen[P2]) = new PolyFunction5[service.Endpoint, P2] {
    def apply[I, E, O, SI, SO](fa: service.Endpoint[I, E, O, SI, SO]): P2[I, E, O, SI, SO] =
    fa match {
      case ExampleServiceOperation.ExampleOperation => algebra.exampleOperation.asInstanceOf[P2[I, E, O, SI, SO]]
    }
  }

  def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: ExampleServiceProductGen[F], f: PolyFunction5[F, G]): ExampleServiceProductGen[G] = {
    new ExampleServiceProductGen[G] {
      def exampleOperation: G[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] = f[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing](alg.exampleOperation)
    }
  }
}

sealed trait ExampleServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ExampleServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ExampleServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ExampleServiceOperation {

  object reified extends ExampleServiceGen[ExampleServiceOperation] {
    def exampleOperation(a: String): ExampleOperation = ExampleOperation(ExampleOperationInput(a))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ExampleServiceGen[P], f: PolyFunction5[P, P1]) extends ExampleServiceGen[P1] {
    def exampleOperation(a: String): P1[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] = f[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing](alg.exampleOperation(a))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ExampleServiceGen[P]): PolyFunction5[ExampleServiceOperation, P] = new PolyFunction5[ExampleServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ExampleServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class ExampleOperation(input: ExampleOperationInput) extends ExampleServiceOperation[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ExampleServiceGen[F]): F[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] = impl.exampleOperation(input.a)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ExampleServiceOperation,ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] = ExampleOperation
  }
  object ExampleOperation extends smithy4s.Endpoint[ExampleServiceOperation,ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] {
    val schema: OperationSchema[ExampleOperationInput, Nothing, ExampleOperationOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.product", "ExampleOperation"))
      .withInput(ExampleOperationInput.schema)
      .withOutput(ExampleOperationOutput.schema)
    def wrap(input: ExampleOperationInput): ExampleOperation = ExampleOperation(input)
  }
}

