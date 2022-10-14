package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.Transformation
import smithy4s.Monadic
import smithy4s.Service
import smithy4s.Hints
import smithy4s.StreamingSchema
import smithy4s.ShapeId
import smithy4s.Endpoint

trait ReservedNameServiceGen[F[_, _, _, _, _]] {
  self =>

  def set(set: Set[String]) : F[SetInput, Nothing, Unit, Nothing, Nothing]
  def list(list: List[String]) : F[ListInput, Nothing, Unit, Nothing, Nothing]
  def map(value: Map[String,String]) : F[MapInput, Nothing, Unit, Nothing, Nothing]
  def option(value: Option[String] = None) : F[OptionInput, Nothing, Unit, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : Transformation[F, G]) : ReservedNameServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends ReservedNameServiceGen[G] {
    def set(set: Set[String]) = transformation[SetInput, Nothing, Unit, Nothing, Nothing](self.set(set))
    def list(list: List[String]) = transformation[ListInput, Nothing, Unit, Nothing, Nothing](self.list(list))
    def map(value: Map[String,String]) = transformation[MapInput, Nothing, Unit, Nothing, Nothing](self.map(value))
    def option(value: Option[String] = None) = transformation[OptionInput, Nothing, Unit, Nothing, Nothing](self.option(value))
  }
}

object ReservedNameServiceGen extends Service[ReservedNameServiceGen, ReservedNameServiceOperation] {

  def apply[F[_]](implicit F: Monadic[ReservedNameServiceGen, F]): F.type = F

  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedNameService")

  val hints : Hints = Hints(
    alloy.SimpleRestJson(),
  )

  val endpoints: List[Endpoint[ReservedNameServiceOperation, _, _, _, _, _]] = List(
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

  def transform[P[_, _, _, _, _]](transformation: Transformation[ReservedNameServiceOperation, P]): ReservedNameServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameServiceGen[P], transformation: Transformation[P, P1]): ReservedNameServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : ReservedNameServiceGen[P]): Transformation[ReservedNameServiceOperation, P] = new Transformation[ReservedNameServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case _Set(SetInput(set)) => impl.set(set)
      case _List(ListInput(list)) => impl.list(list)
      case _Map(MapInput(value)) => impl.map(value)
      case _Option(OptionInput(value)) => impl.option(value)
    }
  }
  case class _Set(input: SetInput) extends ReservedNameServiceOperation[SetInput, Nothing, Unit, Nothing, Nothing]
  object _Set extends Endpoint[ReservedNameServiceOperation, SetInput, Nothing, Unit, Nothing, Nothing] {
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
  object _List extends Endpoint[ReservedNameServiceOperation, ListInput, Nothing, Unit, Nothing, Nothing] {
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
  object _Map extends Endpoint[ReservedNameServiceOperation, MapInput, Nothing, Unit, Nothing, Nothing] {
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
  object _Option extends Endpoint[ReservedNameServiceOperation, OptionInput, Nothing, Unit, Nothing, Nothing] {
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
