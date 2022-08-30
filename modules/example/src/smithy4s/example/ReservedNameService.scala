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

trait ReservedNameServiceGen[F[_, _, _, _, _]] {
  self =>

  def set(set: scala.collection.immutable.Set[StringValue]) : F[SetInput, Nothing, Unit, Nothing, Nothing]
  def list(value: StringValue) : F[ListInput, Nothing, Unit, Nothing, Nothing]
  def map(value: scala.collection.immutable.Map[StringKey,StringValue]) : F[MapInput, Nothing, Unit, Nothing, Nothing]
  def option(value: StringValue) : F[OptionInput, Nothing, Unit, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : Transformation[F, G]) : ReservedNameServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends ReservedNameServiceGen[G] {
    def set(set: scala.collection.immutable.Set[StringValue]) = transformation[SetInput, Nothing, Unit, Nothing, Nothing](self.set(set))
    def list(value: StringValue) = transformation[ListInput, Nothing, Unit, Nothing, Nothing](self.list(value))
    def map(value: scala.collection.immutable.Map[StringKey,StringValue]) = transformation[MapInput, Nothing, Unit, Nothing, Nothing](self.map(value))
    def option(value: StringValue) = transformation[OptionInput, Nothing, Unit, Nothing, Nothing](self.option(value))
  }
}

object ReservedNameServiceGen extends Service[ReservedNameServiceGen, ReservedNameServiceOperation] {

  def apply[F[_]](implicit F: Monadic[ReservedNameServiceGen, F]): F.type = F

  val id: ShapeId = ShapeId("smithy4s.example", "ReservedNameService")

  val hints : Hints = Hints(
    smithy4s.api.SimpleRestJson(),
  )

  val endpoints: scala.List[Endpoint[ReservedNameServiceOperation, _, _, _, _, _]] = scala.List(
    Set,
    List,
    Map,
    Option,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) = op match {
    case Set(input) => (input, Set)
    case List(input) => (input, List)
    case Map(input) => (input, Map)
    case Option(input) => (input, Option)
  }

  object reified extends ReservedNameServiceGen[ReservedNameServiceOperation] {
    def set(set: scala.collection.immutable.Set[StringValue]) = Set(SetInput(set))
    def list(value: StringValue) = List(ListInput(value))
    def map(value: scala.collection.immutable.Map[StringKey,StringValue]) = Map(MapInput(value))
    def option(value: StringValue) = Option(OptionInput(value))
  }

  def transform[P[_, _, _, _, _]](transformation: Transformation[ReservedNameServiceOperation, P]): ReservedNameServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameServiceGen[P], transformation: Transformation[P, P1]): ReservedNameServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : ReservedNameServiceGen[P]): Transformation[ReservedNameServiceOperation, P] = new Transformation[ReservedNameServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case Set(SetInput(set)) => impl.set(set)
      case List(ListInput(value)) => impl.list(value)
      case Map(MapInput(value)) => impl.map(value)
      case Option(OptionInput(value)) => impl.option(value)
    }
  }
  case class Set(input: SetInput) extends ReservedNameServiceOperation[SetInput, Nothing, Unit, Nothing, Nothing]
  object Set extends Endpoint[ReservedNameServiceOperation, SetInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Set")
    val input: Schema[SetInput] = SetInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/set/"), code = 204),
    )
    def wrap(input: SetInput) = Set(input)
  }
  case class List(input: ListInput) extends ReservedNameServiceOperation[ListInput, Nothing, Unit, Nothing, Nothing]
  object List extends Endpoint[ReservedNameServiceOperation, ListInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "List")
    val input: Schema[ListInput] = ListInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/list/{value}"), code = 204),
    )
    def wrap(input: ListInput) = List(input)
  }
  case class Map(input: MapInput) extends ReservedNameServiceOperation[MapInput, Nothing, Unit, Nothing, Nothing]
  object Map extends Endpoint[ReservedNameServiceOperation, MapInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Map")
    val input: Schema[MapInput] = MapInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/map/"), code = 204),
    )
    def wrap(input: MapInput) = Map(input)
  }
  case class Option(input: OptionInput) extends ReservedNameServiceOperation[OptionInput, Nothing, Unit, Nothing, Nothing]
  object Option extends Endpoint[ReservedNameServiceOperation, OptionInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Option")
    val input: Schema[OptionInput] = OptionInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/option/{value}"), code = 204),
    )
    def wrap(input: OptionInput) = Option(input)
  }
}

sealed trait ReservedNameServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
