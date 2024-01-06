package smithy4s.example

import _root_.smithy4s.Endpoint
import _root_.smithy4s.Hints
import _root_.smithy4s.Service
import _root_.smithy4s.ShapeId
import _root_.smithy4s.Transformation
import _root_.smithy4s.kinds.PolyFunction5
import _root_.smithy4s.kinds.toPolyFunction5.const5

trait EmptyServiceGen[F[_, _, _, _, _]] {
  self =>


  def transform: Transformation.PartiallyApplied[EmptyServiceGen[F]] = Transformation.of[EmptyServiceGen[F]](this)
}

object EmptyServiceGen extends Service.Mixin[EmptyServiceGen, EmptyServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "EmptyService")
  val version: String = "1.0"

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[EmptyServiceOperation, _, _, _, _, _]] = Vector()

  def input[I, E, O, SI, SO](op: EmptyServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: EmptyServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: EmptyServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends EmptyServiceOperation.Transformed[EmptyServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: EmptyServiceGen[EmptyServiceOperation] = EmptyServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: EmptyServiceGen[P], f: PolyFunction5[P, P1]): EmptyServiceGen[P1] = new EmptyServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[EmptyServiceOperation, P]): EmptyServiceGen[P] = new EmptyServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: EmptyServiceGen[P]): PolyFunction5[EmptyServiceOperation, P] = EmptyServiceOperation.toPolyFunction(impl)

}

sealed trait EmptyServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: EmptyServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[EmptyServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object EmptyServiceOperation {

  object reified extends EmptyServiceGen[EmptyServiceOperation] {
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: EmptyServiceGen[P], f: PolyFunction5[P, P1]) extends EmptyServiceGen[P1] {
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: EmptyServiceGen[P]): PolyFunction5[EmptyServiceOperation, P] = new PolyFunction5[EmptyServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: EmptyServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = sys.error("impossible")
  }
}

