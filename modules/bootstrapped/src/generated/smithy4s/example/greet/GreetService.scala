package smithy4s.example.greet

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5

trait GreetServiceGen[F[_, _, _, _, _]] {
  self =>

  def greet(name: String): F[GreetInput, Nothing, GreetOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[GreetServiceGen[F]] = Transformation.of[GreetServiceGen[F]](this)
}

object GreetServiceGen extends Service.Mixin[GreetServiceGen, GreetServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetService")
  val version: String = ""

  val hints: Hints =
  Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[GreetServiceOperation, _, _, _, _, _]] = Vector(
    GreetServiceOperation.Greet,
  )

  def input[I, E, O, SI, SO](op: GreetServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: GreetServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: GreetServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends GreetServiceOperation.Transformed[GreetServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: GreetServiceGen[GreetServiceOperation] = GreetServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: GreetServiceGen[P], f: PolyFunction5[P, P1]): GreetServiceGen[P1] = new GreetServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[GreetServiceOperation, P]): GreetServiceGen[P] = new GreetServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: GreetServiceGen[P]): PolyFunction5[GreetServiceOperation, P] = GreetServiceOperation.toPolyFunction(impl)

}

sealed trait GreetServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: GreetServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[GreetServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object GreetServiceOperation {

  object reified extends GreetServiceGen[GreetServiceOperation] {
    def greet(name: String) = Greet(GreetInput(name))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: GreetServiceGen[P], f: PolyFunction5[P, P1]) extends GreetServiceGen[P1] {
    def greet(name: String) = f[GreetInput, Nothing, GreetOutput, Nothing, Nothing](alg.greet(name))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: GreetServiceGen[P]): PolyFunction5[GreetServiceOperation, P] = new PolyFunction5[GreetServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: GreetServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Greet(input: GreetInput) extends GreetServiceOperation[GreetInput, Nothing, GreetOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: GreetServiceGen[F]): F[GreetInput, Nothing, GreetOutput, Nothing, Nothing] = impl.greet(input.name)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[GreetServiceOperation,GreetInput, Nothing, GreetOutput, Nothing, Nothing] = Greet
  }
  object Greet extends smithy4s.Endpoint[GreetServiceOperation,GreetInput, Nothing, GreetOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.greet", "Greet")
    val input: Schema[GreetInput] = GreetInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GreetOutput] = GreetOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints.empty
    def wrap(input: GreetInput) = Greet(input)
    override val errorable: Option[Nothing] = None
  }
}

