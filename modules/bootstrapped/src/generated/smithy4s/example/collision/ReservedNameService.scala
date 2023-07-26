package smithy4s.example.collision

import alloy.SimpleRestJson
import smithy.api.Http
import smithy.api.NonEmptyString
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

trait ReservedNameServiceGen[F[_, _, _, _, _]] {
  self =>

  def set(set: scala.collection.immutable.Set[smithy4s.example.collision.String]): F[SetInput, Nothing, Unit, Nothing, Nothing]
  def list(list: scala.List[smithy4s.example.collision.String]): F[ListInput, Nothing, Unit, Nothing, Nothing]
  def map(value: scala.collection.immutable.Map[smithy4s.example.collision.String, smithy4s.example.collision.String]): F[MapInput, Nothing, Unit, Nothing, Nothing]
  def option(value: scala.Option[smithy4s.example.collision.String] = None): F[OptionInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ReservedNameServiceGen[F]] = Transformation.of[ReservedNameServiceGen[F]](this)
}

object ReservedNameServiceGen extends Service.Mixin[ReservedNameServiceGen, ReservedNameServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedNameService")
  val version: java.lang.String = "1.0.0"

  val hints: Hints = Hints(
    SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ReservedNameServiceOperation, _, _, _, _, _]] = Vector(
    ReservedNameServiceOperation.Set,
    ReservedNameServiceOperation.List,
    ReservedNameServiceOperation.Map,
    ReservedNameServiceOperation.Option,
  )

  def input[I, E, O, SI, SO](op: ReservedNameServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ReservedNameServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ReservedNameServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ReservedNameServiceOperation.Transformed[ReservedNameServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ReservedNameServiceGen[ReservedNameServiceOperation] = ReservedNameServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameServiceGen[P], f: PolyFunction5[P, P1]): ReservedNameServiceGen[P1] = new ReservedNameServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ReservedNameServiceOperation, P]): ReservedNameServiceGen[P] = new ReservedNameServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ReservedNameServiceGen[P]): PolyFunction5[ReservedNameServiceOperation, P] = ReservedNameServiceOperation.toPolyFunction(impl)

}

sealed trait ReservedNameServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ReservedNameServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ReservedNameServiceOperation {

  object reified extends ReservedNameServiceGen[ReservedNameServiceOperation] {
    def set(set: scala.collection.immutable.Set[smithy4s.example.collision.String]) = Set(SetInput(set))
    def list(list: scala.List[smithy4s.example.collision.String]) = List(ListInput(list))
    def map(value: scala.collection.immutable.Map[smithy4s.example.collision.String, smithy4s.example.collision.String]) = Map(MapInput(value))
    def option(value: scala.Option[smithy4s.example.collision.String] = None) = Option(OptionInput(value))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ReservedNameServiceGen[P], f: PolyFunction5[P, P1]) extends ReservedNameServiceGen[P1] {
    def set(set: scala.collection.immutable.Set[smithy4s.example.collision.String]) = f[SetInput, Nothing, Unit, Nothing, Nothing](alg.set(set))
    def list(list: scala.List[smithy4s.example.collision.String]) = f[ListInput, Nothing, Unit, Nothing, Nothing](alg.list(list))
    def map(value: scala.collection.immutable.Map[smithy4s.example.collision.String, smithy4s.example.collision.String]) = f[MapInput, Nothing, Unit, Nothing, Nothing](alg.map(value))
    def option(value: scala.Option[smithy4s.example.collision.String] = None) = f[OptionInput, Nothing, Unit, Nothing, Nothing](alg.option(value))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ReservedNameServiceGen[P]): PolyFunction5[ReservedNameServiceOperation, P] = new PolyFunction5[ReservedNameServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ReservedNameServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Set(input: SetInput) extends ReservedNameServiceOperation[SetInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[SetInput, Nothing, Unit, Nothing, Nothing] = impl.set(input.set)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[ReservedNameServiceOperation,SetInput, Nothing, Unit, Nothing, Nothing] = Set
  }
  object Set extends smithy4s.Endpoint[ReservedNameServiceOperation,SetInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Set")
    val input: Schema[SetInput] = SetInput.$schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/api/set/"), code = 204),
    )
    def wrap(input: SetInput) = Set(input)
    override val errorable: scala.Option[Nothing] = None
  }
  final case class List(input: ListInput) extends ReservedNameServiceOperation[ListInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[ListInput, Nothing, Unit, Nothing, Nothing] = impl.list(input.list)
    def ordinal = 1
    def endpoint: smithy4s.Endpoint[ReservedNameServiceOperation,ListInput, Nothing, Unit, Nothing, Nothing] = List
  }
  object List extends smithy4s.Endpoint[ReservedNameServiceOperation,ListInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "List")
    val input: Schema[ListInput] = ListInput.$schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/api/list/"), code = 204),
    )
    def wrap(input: ListInput) = List(input)
    override val errorable: scala.Option[Nothing] = None
  }
  final case class Map(input: MapInput) extends ReservedNameServiceOperation[MapInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[MapInput, Nothing, Unit, Nothing, Nothing] = impl.map(input.value)
    def ordinal = 2
    def endpoint: smithy4s.Endpoint[ReservedNameServiceOperation,MapInput, Nothing, Unit, Nothing, Nothing] = Map
  }
  object Map extends smithy4s.Endpoint[ReservedNameServiceOperation,MapInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Map")
    val input: Schema[MapInput] = MapInput.$schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/api/map/"), code = 204),
    )
    def wrap(input: MapInput) = Map(input)
    override val errorable: scala.Option[Nothing] = None
  }
  final case class Option(input: OptionInput) extends ReservedNameServiceOperation[OptionInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[OptionInput, Nothing, Unit, Nothing, Nothing] = impl.option(input.value)
    def ordinal = 3
    def endpoint: smithy4s.Endpoint[ReservedNameServiceOperation,OptionInput, Nothing, Unit, Nothing, Nothing] = Option
  }
  object Option extends smithy4s.Endpoint[ReservedNameServiceOperation,OptionInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Option")
    val input: Schema[OptionInput] = OptionInput.$schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/api/option/"), code = 204),
    )
    def wrap(input: OptionInput) = Option(input)
    override val errorable: scala.Option[Nothing] = None
  }
}
