package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.StaticService
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

trait FooServiceStaticGen[F[_, _, _, _, _]] {
  self =>

  /** Returns a useful Foo
    * No input necessary to find our Foo
    * The path for this operation is "/foo"
    */
  def getFoo: F[Unit, Nothing, GetFooOutput, Nothing, Nothing]
}

object FooServiceGen extends Service.Mixin[FooServiceGen, FooServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "FooService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    smithy.api.Documentation("The most basics of services\nGetFoo is its only operation"),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: List[smithy4s.Endpoint[FooServiceOperation, _, _, _, _, _]] = List(
    FooServiceOperation.GetFoo,
  )

  def endpoint[I, E, O, SI, SO](op: FooServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends FooServiceOperation.Transformed[FooServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: FooServiceGen[FooServiceOperation] = FooServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: FooServiceGen[P], f: PolyFunction5[P, P1]): FooServiceGen[P1] = new FooServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[FooServiceOperation, P]): FooServiceGen[P] = new FooServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: FooServiceGen[P]): PolyFunction5[FooServiceOperation, P] = FooServiceOperation.toPolyFunction(impl)

  type StaticAlg[F[_, _, _, _, _]] = FooServiceStaticGen[F]
  val static: StaticService.Aux[FooServiceStaticGen, FooServiceGen] = FooServiceStaticGen
}

object FooServiceStaticGen extends StaticService[FooServiceStaticGen] {
  type Alg[F[_, _, _, _, _]] = FooServiceGen[F]
  val service: FooServiceGen.type = FooServiceGen

  def endpoints: FooServiceStaticGen[service.Endpoint] = new FooServiceStaticGen[service.Endpoint] {
    def getFoo: service.Endpoint[Unit, Nothing, GetFooOutput, Nothing, Nothing] = FooServiceOperation.GetFoo
  }

  def toPolyFunction[P2[_, _, _, _, _]](algebra: FooServiceStaticGen[P2]) = new PolyFunction5[service.Endpoint, P2] {
    def apply[A0, A1, A2, A3, A4](fa: service.Endpoint[A0, A1, A2, A3, A4]): P2[A0, A1, A2, A3, A4] =
    fa match {
      case FooServiceOperation.GetFoo => algebra.getFoo
    }
  }

  def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: FooServiceStaticGen[F], f: PolyFunction5[F, G]): FooServiceStaticGen[G] = {
    new FooServiceStaticGen[G] {
      def getFoo: G[Unit, Nothing, GetFooOutput, Nothing, Nothing] = f[Unit, Nothing, GetFooOutput, Nothing, Nothing](alg.getFoo)
    }
  }
}

sealed trait FooServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: FooServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[FooServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}

object FooServiceOperation {

  object reified extends FooServiceGen[FooServiceOperation] {
    def getFoo() = GetFoo()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: FooServiceGen[P], f: PolyFunction5[P, P1]) extends FooServiceGen[P1] {
    def getFoo() = f[Unit, Nothing, GetFooOutput, Nothing, Nothing](alg.getFoo())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: FooServiceGen[P]): PolyFunction5[FooServiceOperation, P] = new PolyFunction5[FooServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: FooServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetFoo() extends FooServiceOperation[Unit, Nothing, GetFooOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: FooServiceGen[F]): F[Unit, Nothing, GetFooOutput, Nothing, Nothing] = impl.getFoo()
    def endpoint: (Unit, smithy4s.Endpoint[FooServiceOperation,Unit, Nothing, GetFooOutput, Nothing, Nothing]) = ((), GetFoo)
  }
  object GetFoo extends smithy4s.Endpoint[FooServiceOperation,Unit, Nothing, GetFooOutput, Nothing, Nothing] {
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
    override val errorable: Option[Nothing] = None
  }
}

