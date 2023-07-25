package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.unit

trait RecursiveInputServiceGen[F[_, _, _, _, _]] {
  self =>

  def recursiveInputOperation(hello: Option[RecursiveInput] = None): F[RecursiveInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[RecursiveInputServiceGen[F]] = Transformation.of[RecursiveInputServiceGen[F]](this)
}

object RecursiveInputServiceGen extends Service.Mixin[RecursiveInputServiceGen, RecursiveInputServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveInputService")
  val version: String = "0.0.1"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[RecursiveInputServiceOperation, _, _, _, _, _]] = Vector(
    RecursiveInputServiceOperation.RecursiveInputOperation,
  )

  def input[I, E, O, SI, SO](op: RecursiveInputServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: RecursiveInputServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: RecursiveInputServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends RecursiveInputServiceOperation.Transformed[RecursiveInputServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: RecursiveInputServiceGen[RecursiveInputServiceOperation] = RecursiveInputServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: RecursiveInputServiceGen[P], f: PolyFunction5[P, P1]): RecursiveInputServiceGen[P1] = new RecursiveInputServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[RecursiveInputServiceOperation, P]): RecursiveInputServiceGen[P] = new RecursiveInputServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: RecursiveInputServiceGen[P]): PolyFunction5[RecursiveInputServiceOperation, P] = RecursiveInputServiceOperation.toPolyFunction(impl)

}

sealed trait RecursiveInputServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: RecursiveInputServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[RecursiveInputServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object RecursiveInputServiceOperation {

  object reified extends RecursiveInputServiceGen[RecursiveInputServiceOperation] {
    def recursiveInputOperation(hello: Option[RecursiveInput] = None) = RecursiveInputOperation(RecursiveInput(hello))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: RecursiveInputServiceGen[P], f: PolyFunction5[P, P1]) extends RecursiveInputServiceGen[P1] {
    def recursiveInputOperation(hello: Option[RecursiveInput] = None) = f[RecursiveInput, Nothing, Unit, Nothing, Nothing](alg.recursiveInputOperation(hello))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: RecursiveInputServiceGen[P]): PolyFunction5[RecursiveInputServiceOperation, P] = new PolyFunction5[RecursiveInputServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: RecursiveInputServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class RecursiveInputOperation(input: RecursiveInput) extends RecursiveInputServiceOperation[RecursiveInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: RecursiveInputServiceGen[F]): F[RecursiveInput, Nothing, Unit, Nothing, Nothing] = impl.recursiveInputOperation(input.hello)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[RecursiveInputServiceOperation,RecursiveInput, Nothing, Unit, Nothing, Nothing] = RecursiveInputOperation
  }
  object RecursiveInputOperation extends smithy4s.Endpoint[RecursiveInputServiceOperation,RecursiveInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "RecursiveInputOperation")
    val input: Schema[RecursiveInput] = RecursiveInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("PUT"), uri = smithy.api.NonEmptyString("/subscriptions"), code = 200),
      smithy.api.Idempotent(),
    )
    def wrap(input: RecursiveInput) = RecursiveInputOperation(input)
    override val errorable: Option[Nothing] = None
  }
}

