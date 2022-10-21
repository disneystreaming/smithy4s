package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.Transformation
import smithy4s.Monadic
import smithy4s.Service
import smithy4s.Hints
import smithy4s.StreamingSchema
import smithy4s.ShapeId
import smithy4s.Endpoint

trait FooServiceGen[F[_, _, _, _, _]] {
  self =>

  def getFoo() : F[Unit, Nothing, GetFooOutput, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[FooServiceGen, F] = new Transformation.PartiallyApplied[FooServiceGen, F](this)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends FooServiceGen[G] {
    def getFoo() = transformation[Unit, Nothing, GetFooOutput, Nothing, Nothing](self.getFoo())
  }
}

object FooServiceGen extends Service[FooServiceGen, FooServiceOperation] {

  def apply[F[_]](implicit F: Monadic[FooServiceGen, F]): F.type = F

  val id: ShapeId = ShapeId("smithy4s.example", "FooService")

  val hints : Hints = Hints.empty

  val endpoints: List[Endpoint[FooServiceOperation, _, _, _, _, _]] = List(
    GetFoo,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : FooServiceOperation[I, E, O, SI, SO]) = op match {
    case GetFoo() => ((), GetFoo)
  }

  object reified extends FooServiceGen[FooServiceOperation] {
    def getFoo() = GetFoo()
  }

  def transform[P[_, _, _, _, _]](transformation: Transformation[FooServiceOperation, P]): FooServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: FooServiceGen[P], transformation: Transformation[P, P1]): FooServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : FooServiceGen[P]): Transformation[FooServiceOperation, P] = new Transformation[FooServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : FooServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case GetFoo() => impl.getFoo()
    }
  }
  case class GetFoo() extends FooServiceOperation[Unit, Nothing, GetFooOutput, Nothing, Nothing]
  object GetFoo extends Endpoint[FooServiceOperation, Unit, Nothing, GetFooOutput, Nothing, Nothing] {
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
