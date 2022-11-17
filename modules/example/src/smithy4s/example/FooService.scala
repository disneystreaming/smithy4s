package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.kinds.PolyFunction5
import smithy4s.Transformation
import smithy4s.kinds.FunctorAlgebra
import smithy4s.ShapeId
import smithy4s.Service
import smithy4s.kinds.BiFunctorAlgebra
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.Hints
import smithy4s.StreamingSchema

trait FooServiceGen[F[_, _, _, _, _]] {
  self =>

  def getFoo() : F[Unit, Nothing, GetFooOutput, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[FooServiceGen[F]] = new Transformation.PartiallyApplied[FooServiceGen[F]](this)
}

object FooServiceGen extends Service.Mixin[FooServiceGen, FooServiceOperation] {

  def apply[F[_]](implicit F: FunctorAlgebra[FooServiceGen, F]): F.type = F

  type WithError[F[_, _]] = BiFunctorAlgebra[FooServiceGen, F]
  object WithError {
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val id: ShapeId = ShapeId("smithy4s.example", "FooService")

  val hints : Hints = Hints.empty

  val endpoints: List[FooServiceGen.Endpoint[_, _, _, _, _]] = List(
    GetFoo,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : FooServiceOperation[I, E, O, SI, SO]) = op match {
    case GetFoo() => ((), GetFoo)
  }

  object reified extends FooServiceGen[FooServiceOperation] {
    def getFoo() = GetFoo()
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: FooServiceGen[P], f: PolyFunction5[P, P1]): FooServiceGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[FooServiceOperation, P]): FooServiceGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: FooServiceGen[P], f : PolyFunction5[P, P1]) extends FooServiceGen[P1] {
    def getFoo() = f[Unit, Nothing, GetFooOutput, Nothing, Nothing](alg.getFoo())
  }

  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends Transformed[FooServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]

  def toPolyFunction[P[_, _, _, _, _]](impl : FooServiceGen[P]): PolyFunction5[FooServiceOperation, P] = new PolyFunction5[FooServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : FooServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case GetFoo() => impl.getFoo()
    }
  }
  case class GetFoo() extends FooServiceOperation[Unit, Nothing, GetFooOutput, Nothing, Nothing]
  object GetFoo extends FooServiceGen.Endpoint[Unit, Nothing, GetFooOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetFoo")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetFooOutput] = GetFooOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/foo"), code = 200),
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = GetFoo()
  }
}

sealed trait FooServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
