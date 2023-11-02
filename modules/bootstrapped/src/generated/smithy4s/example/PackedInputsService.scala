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

trait PackedInputsServiceGen[F[_, _, _, _, _]] {
  self =>

  def packedInputOperation(input: PackedInput): F[PackedInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[PackedInputsServiceGen[F]] = Transformation.of[PackedInputsServiceGen[F]](this)
}

object PackedInputsServiceGen extends Service.Mixin[PackedInputsServiceGen, PackedInputsServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "PackedInputsService")
  val version: String = "1.0.0"

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[PackedInputsServiceOperation, _, _, _, _, _]] = Vector(
    PackedInputsServiceOperation.PackedInputOperation,
  )

  def input[I, E, O, SI, SO](op: PackedInputsServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: PackedInputsServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: PackedInputsServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends PackedInputsServiceOperation.Transformed[PackedInputsServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: PackedInputsServiceGen[PackedInputsServiceOperation] = PackedInputsServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: PackedInputsServiceGen[P], f: PolyFunction5[P, P1]): PackedInputsServiceGen[P1] = new PackedInputsServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[PackedInputsServiceOperation, P]): PackedInputsServiceGen[P] = new PackedInputsServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: PackedInputsServiceGen[P]): PolyFunction5[PackedInputsServiceOperation, P] = PackedInputsServiceOperation.toPolyFunction(impl)

}

sealed trait PackedInputsServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: PackedInputsServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[PackedInputsServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object PackedInputsServiceOperation {

  object reified extends PackedInputsServiceGen[PackedInputsServiceOperation] {
    def packedInputOperation(input: PackedInput): PackedInputOperation = PackedInputOperation(input)
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: PackedInputsServiceGen[P], f: PolyFunction5[P, P1]) extends PackedInputsServiceGen[P1] {
    def packedInputOperation(input: PackedInput): P1[PackedInput, Nothing, Unit, Nothing, Nothing] = f[PackedInput, Nothing, Unit, Nothing, Nothing](alg.packedInputOperation(input))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: PackedInputsServiceGen[P]): PolyFunction5[PackedInputsServiceOperation, P] = new PolyFunction5[PackedInputsServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: PackedInputsServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class PackedInputOperation(input: PackedInput) extends PackedInputsServiceOperation[PackedInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PackedInputsServiceGen[F]): F[PackedInput, Nothing, Unit, Nothing, Nothing] = impl.packedInputOperation(input)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[PackedInputsServiceOperation,PackedInput, Nothing, Unit, Nothing, Nothing] = PackedInputOperation
  }
  object PackedInputOperation extends smithy4s.Endpoint[PackedInputsServiceOperation,PackedInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[PackedInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "PackedInputOperation"))
      .withInput(PackedInput.schema)
      .withOutput(unit)
      .withHints()
    def wrap(input: PackedInput): PackedInputOperation = PackedInputOperation(input)
  }
}

