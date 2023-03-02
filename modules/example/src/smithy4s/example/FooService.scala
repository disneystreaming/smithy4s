package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.unit

/** The most basics of services
  * GetFoo is its only operation
  */
trait FooServiceGen[F[_, _, _, _, _]] {
  self =>

  /** Returns a useful Foo
    * No input necessary to find our Foo
    * The path for this operation is "/foo"
    */
  def getFoo(): F[Unit, Nothing, GetFooOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[FooServiceGen[F]] = Transformation.of[FooServiceGen[F]](this)
}

object FooServiceGen extends Service.Mixin[FooServiceGen, FooServiceOperation] {

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val id: ShapeId = ShapeId("smithy4s.example", "FooService")

  val hints: Hints = Hints(
    smithy.api.Documentation("The most basics of services\nGetFoo is its only operation"),
  )

  val endpoints: List[FooServiceGen.Endpoint[_, _, _, _, _]] = List(
    GetFoo,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op: FooServiceOperation[I, E, O, SI, SO]) = op.endpoint

  object reified extends FooServiceGen[FooServiceOperation] {
    def getFoo() = GetFoo()
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: FooServiceGen[P], f: PolyFunction5[P, P1]): FooServiceGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[FooServiceOperation, P]): FooServiceGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: FooServiceGen[P], f: PolyFunction5[P, P1]) extends FooServiceGen[P1] {
    def getFoo() = f[Unit, Nothing, GetFooOutput, Nothing, Nothing](alg.getFoo())
  }

  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends Transformed[FooServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]

  def toPolyFunction[P[_, _, _, _, _]](impl: FooServiceGen[P]): PolyFunction5[FooServiceOperation, P] = new PolyFunction5[FooServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: FooServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  case class GetFoo() extends FooServiceOperation[Unit, Nothing, GetFooOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: FooServiceGen[F]): F[Unit, Nothing, GetFooOutput, Nothing, Nothing] = impl.getFoo()
    def endpoint: (Unit, Endpoint[Unit, Nothing, GetFooOutput, Nothing, Nothing]) = ((), GetFoo)
  }
  object GetFoo extends FooServiceGen.Endpoint[Unit, Nothing, GetFooOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetFoo")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetFooOutput] = GetFooOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/foo"), code = 200),
      smithy.api.Documentation("Returns a useful Foo\nNo input necessary to find our Foo\nThe path for this operation is \"/foo\""),
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = GetFoo()
  }
}

sealed trait FooServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: FooServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, smithy4s.Endpoint[FooServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}
