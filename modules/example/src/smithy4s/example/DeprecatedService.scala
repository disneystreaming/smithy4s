package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.kinds.PolyFunction5
import smithy4s.Transformation
import smithy4s.kinds.FunctorAlgebra
import smithy4s.ShapeId
import smithy4s.Service
import smithy4s.kinds.BiFunctorAlgebra
import smithy4s.Hints
import smithy4s.StreamingSchema

trait DeprecatedServiceGen[F[_, _, _, _, _]] {
  self =>

  def deprecatedOperation() : F[Unit, Nothing, Unit, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[DeprecatedServiceGen[F]] = new Transformation.PartiallyApplied[DeprecatedServiceGen[F]](this)
}

object DeprecatedServiceGen extends Service.Mixin[DeprecatedServiceGen, DeprecatedServiceOperation] {

  def apply[F[_]](implicit F: FunctorAlgebra[DeprecatedServiceGen, F]): F.type = F

  type WithError[F[_, _]] = BiFunctorAlgebra[DeprecatedServiceGen, F]

  val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedService")

  val hints : Hints = Hints(
    smithy.api.Deprecated(message = None, since = None),
  )

  val endpoints: List[DeprecatedServiceGen.Endpoint[_, _, _, _, _]] = List(
    DeprecatedOperation,
  )

  val version: String = ""

  def endpoint[I, E, O, SI, SO](op : DeprecatedServiceOperation[I, E, O, SI, SO]) = op match {
    case DeprecatedOperation() => ((), DeprecatedOperation)
  }

  object reified extends DeprecatedServiceGen[DeprecatedServiceOperation] {
    def deprecatedOperation() = DeprecatedOperation()
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: DeprecatedServiceGen[P], f: PolyFunction5[P, P1]): DeprecatedServiceGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[DeprecatedServiceOperation, P]): DeprecatedServiceGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: DeprecatedServiceGen[P], f : PolyFunction5[P, P1]) extends DeprecatedServiceGen[P1] {
    def deprecatedOperation() = f[Unit, Nothing, Unit, Nothing, Nothing](alg.deprecatedOperation())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl : DeprecatedServiceGen[P]): PolyFunction5[DeprecatedServiceOperation, P] = new PolyFunction5[DeprecatedServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : DeprecatedServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case DeprecatedOperation() => impl.deprecatedOperation()
    }
  }
  case class DeprecatedOperation() extends DeprecatedServiceOperation[Unit, Nothing, Unit, Nothing, Nothing]
  object DeprecatedOperation extends DeprecatedServiceGen.Endpoint[Unit, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedOperation")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Deprecated(message = None, since = None),
    )
    def wrap(input: Unit) = DeprecatedOperation()
  }
}

sealed trait DeprecatedServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
