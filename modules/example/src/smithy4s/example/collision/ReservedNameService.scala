package smithy4s.example.collision

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

trait ReservedNameServiceGen[F[_, _, _, _, _]] {
  self =>

  def set(set: Set[String]) : F[SetInput, Nothing, Unit, Nothing, Nothing]
  def list(list: List[String]) : F[ListInput, Nothing, Unit, Nothing, Nothing]
  def map(value: Map[String,String]) : F[MapInput, Nothing, Unit, Nothing, Nothing]
  def option(value: Option[String] = None) : F[OptionInput, Nothing, Unit, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[ReservedNameServiceGen[F]] = new Transformation.PartiallyApplied[ReservedNameServiceGen[F]](this)
}

object ReservedNameServiceGen extends Service.Mixin[ReservedNameServiceGen, ReservedNameServiceOperation] {

  def apply[F[_]](implicit F: FunctorAlgebra[ReservedNameServiceGen, F]): F.type = F

  type WithError[F[_, _]] = BiFunctorAlgebra[ReservedNameServiceGen, F]

  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedNameService")

  val hints : Hints = Hints(
    alloy.SimpleRestJson(),
  )

  val endpoints: List[ReservedNameServiceGen.Endpoint[_, _, _, _, _]] = List(
    _Set,
    _List,
    _Map,
    _Option,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) = op match {
    case _Set(input) => (input, _Set)
    case _List(input) => (input, _List)
    case _Map(input) => (input, _Map)
    case _Option(input) => (input, _Option)
  }

  object reified extends ReservedNameServiceGen[ReservedNameServiceOperation] {
    def set(set: Set[String]) = _Set(SetInput(set))
    def list(list: List[String]) = _List(ListInput(list))
    def map(value: Map[String,String]) = _Map(MapInput(value))
    def option(value: Option[String] = None) = _Option(OptionInput(value))
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameServiceGen[P], f: PolyFunction5[P, P1]): ReservedNameServiceGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ReservedNameServiceOperation, P]): ReservedNameServiceGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ReservedNameServiceGen[P], f : PolyFunction5[P, P1]) extends ReservedNameServiceGen[P1] {
    def set(set: Set[String]) = f[SetInput, Nothing, Unit, Nothing, Nothing](alg.set(set))
    def list(list: List[String]) = f[ListInput, Nothing, Unit, Nothing, Nothing](alg.list(list))
    def map(value: Map[String,String]) = f[MapInput, Nothing, Unit, Nothing, Nothing](alg.map(value))
    def option(value: Option[String] = None) = f[OptionInput, Nothing, Unit, Nothing, Nothing](alg.option(value))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl : ReservedNameServiceGen[P]): PolyFunction5[ReservedNameServiceOperation, P] = new PolyFunction5[ReservedNameServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case _Set(SetInput(set)) => impl.set(set)
      case _List(ListInput(list)) => impl.list(list)
      case _Map(MapInput(value)) => impl.map(value)
      case _Option(OptionInput(value)) => impl.option(value)
    }
  }
  case class _Set(input: SetInput) extends ReservedNameServiceOperation[SetInput, Nothing, Unit, Nothing, Nothing]
  object _Set extends ReservedNameServiceGen.Endpoint[SetInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Set")
    val input: Schema[SetInput] = SetInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/set/"), code = 204),
    )
    def wrap(input: SetInput) = _Set(input)
  }
  case class _List(input: ListInput) extends ReservedNameServiceOperation[ListInput, Nothing, Unit, Nothing, Nothing]
  object _List extends ReservedNameServiceGen.Endpoint[ListInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "List")
    val input: Schema[ListInput] = ListInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/list/"), code = 204),
    )
    def wrap(input: ListInput) = _List(input)
  }
  case class _Map(input: MapInput) extends ReservedNameServiceOperation[MapInput, Nothing, Unit, Nothing, Nothing]
  object _Map extends ReservedNameServiceGen.Endpoint[MapInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Map")
    val input: Schema[MapInput] = MapInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/map/"), code = 204),
    )
    def wrap(input: MapInput) = _Map(input)
  }
  case class _Option(input: OptionInput) extends ReservedNameServiceOperation[OptionInput, Nothing, Unit, Nothing, Nothing]
  object _Option extends ReservedNameServiceGen.Endpoint[OptionInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Option")
    val input: Schema[OptionInput] = OptionInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/option/"), code = 204),
    )
    def wrap(input: OptionInput) = _Option(input)
  }
}

sealed trait ReservedNameServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
