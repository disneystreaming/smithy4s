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

trait RoutingServiceGen[F[_, _, _, _, _]] {
  self =>

  def greedyAbcDef(abc: String): F[GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing]
  def abcLabel(_def: String): F[AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing]
  def abcDef(): F[Unit, Nothing, MessageOutput, Nothing, Nothing]
  def abc(): F[Unit, Nothing, MessageOutput, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[RoutingServiceGen[F]] = Transformation.of[RoutingServiceGen[F]](this)
}

object RoutingServiceGen extends Service.Mixin[RoutingServiceGen, RoutingServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "RoutingService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[RoutingServiceOperation, _, _, _, _, _]] = Vector(
    RoutingServiceOperation.GreedyAbcDef,
    RoutingServiceOperation.AbcLabel,
    RoutingServiceOperation.AbcDef,
    RoutingServiceOperation.Abc,
  )

  def input[I, E, O, SI, SO](op: RoutingServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: RoutingServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: RoutingServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends RoutingServiceOperation.Transformed[RoutingServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: RoutingServiceGen[RoutingServiceOperation] = RoutingServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: RoutingServiceGen[P], f: PolyFunction5[P, P1]): RoutingServiceGen[P1] = new RoutingServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[RoutingServiceOperation, P]): RoutingServiceGen[P] = new RoutingServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: RoutingServiceGen[P]): PolyFunction5[RoutingServiceOperation, P] = RoutingServiceOperation.toPolyFunction(impl)

}

sealed trait RoutingServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: RoutingServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[RoutingServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object RoutingServiceOperation {

  object reified extends RoutingServiceGen[RoutingServiceOperation] {
    def greedyAbcDef(abc: String): GreedyAbcDef = GreedyAbcDef(GreedyAbcDefInput(abc))
    def abcLabel(_def: String): AbcLabel = AbcLabel(AbcLabelInput(_def))
    def abcDef(): AbcDef = AbcDef()
    def abc(): Abc = Abc()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: RoutingServiceGen[P], f: PolyFunction5[P, P1]) extends RoutingServiceGen[P1] {
    def greedyAbcDef(abc: String): P1[GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing] = f[GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing](alg.greedyAbcDef(abc))
    def abcLabel(_def: String): P1[AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing] = f[AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing](alg.abcLabel(_def))
    def abcDef(): P1[Unit, Nothing, MessageOutput, Nothing, Nothing] = f[Unit, Nothing, MessageOutput, Nothing, Nothing](alg.abcDef())
    def abc(): P1[Unit, Nothing, MessageOutput, Nothing, Nothing] = f[Unit, Nothing, MessageOutput, Nothing, Nothing](alg.abc())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: RoutingServiceGen[P]): PolyFunction5[RoutingServiceOperation, P] = new PolyFunction5[RoutingServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: RoutingServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GreedyAbcDef(input: GreedyAbcDefInput) extends RoutingServiceOperation[GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: RoutingServiceGen[F]): F[GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing] = impl.greedyAbcDef(input.abc)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[RoutingServiceOperation,GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing] = GreedyAbcDef
  }
  object GreedyAbcDef extends smithy4s.Endpoint[RoutingServiceOperation,GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing] {
    val schema: OperationSchema[GreedyAbcDefInput, Nothing, MessageOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GreedyAbcDef"))
      .withInput(GreedyAbcDefInput.schema)
      .withOutput(MessageOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/{abc+}/def"), code = 200), smithy.api.Readonly())
    def wrap(input: GreedyAbcDefInput): GreedyAbcDef = GreedyAbcDef(input)
  }
  final case class AbcLabel(input: AbcLabelInput) extends RoutingServiceOperation[AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: RoutingServiceGen[F]): F[AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing] = impl.abcLabel(input._def)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[RoutingServiceOperation,AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing] = AbcLabel
  }
  object AbcLabel extends smithy4s.Endpoint[RoutingServiceOperation,AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing] {
    val schema: OperationSchema[AbcLabelInput, Nothing, MessageOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "AbcLabel"))
      .withInput(AbcLabelInput.schema)
      .withOutput(MessageOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/abc/{def}"), code = 200), smithy.api.Readonly())
    def wrap(input: AbcLabelInput): AbcLabel = AbcLabel(input)
  }
  final case class AbcDef() extends RoutingServiceOperation[Unit, Nothing, MessageOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: RoutingServiceGen[F]): F[Unit, Nothing, MessageOutput, Nothing, Nothing] = impl.abcDef()
    def ordinal: Int = 2
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[RoutingServiceOperation,Unit, Nothing, MessageOutput, Nothing, Nothing] = AbcDef
  }
  object AbcDef extends smithy4s.Endpoint[RoutingServiceOperation,Unit, Nothing, MessageOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, MessageOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "AbcDef"))
      .withInput(unit)
      .withOutput(MessageOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/abc/def"), code = 200), smithy.api.Readonly())
    def wrap(input: Unit): AbcDef = AbcDef()
  }
  final case class Abc() extends RoutingServiceOperation[Unit, Nothing, MessageOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: RoutingServiceGen[F]): F[Unit, Nothing, MessageOutput, Nothing, Nothing] = impl.abc()
    def ordinal: Int = 3
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[RoutingServiceOperation,Unit, Nothing, MessageOutput, Nothing, Nothing] = Abc
  }
  object Abc extends smithy4s.Endpoint[RoutingServiceOperation,Unit, Nothing, MessageOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, MessageOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Abc"))
      .withInput(unit)
      .withOutput(MessageOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/abc"), code = 200), smithy.api.Readonly())
    def wrap(input: Unit): Abc = Abc()
  }
}

