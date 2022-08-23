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

  def _set(key: Key, value: Option[Value] = None) : F[SetInput, Nothing, Unit, Nothing, Nothing]
  def _list(value: Value) : F[ListInput, Nothing, Unit, Nothing, Nothing]
  def _option(value: Value) : F[OptionInput, Nothing, Unit, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : Transformation[F, G]) : ReservedNameServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends ReservedNameServiceGen[G] {
    def _set(key: Key, value: Option[Value] = None) = transformation[SetInput, Nothing, Unit, Nothing, Nothing](self._set(key, value))
    def _list(value: Value) = transformation[ListInput, Nothing, Unit, Nothing, Nothing](self._list(value))
    def _option(value: Value) = transformation[OptionInput, Nothing, Unit, Nothing, Nothing](self._option(value))
  }
}

object ReservedNameServiceGen extends Service[ReservedNameServiceGen, ReservedNameServiceOperation] {

  def apply[F[_]](implicit F: Monadic[ReservedNameServiceGen, F]): F.type = F

  val id: ShapeId = ShapeId("smithy4s.example", "ReservedNameService")

  val hints : Hints = Hints(
    smithy4s.api.SimpleRestJson(),
  )

  val endpoints: List[Endpoint[ReservedNameServiceOperation, _, _, _, _, _]] = List(
    _Set,
    _List,
    _Option,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) = op match {
    case _Set(input) => (input, _Set)
    case _List(input) => (input, _List)
    case _Option(input) => (input, _Option)
  }

  object reified extends ReservedNameServiceGen[ReservedNameServiceOperation] {
    def _set(key: Key, value: Option[Value] = None) = _Set(SetInput(key, value))
    def _list(value: Value) = _List(ListInput(value))
    def _option(value: Value) = _Option(OptionInput(value))
  }

  def transform[P[_, _, _, _, _]](transformation: Transformation[ReservedNameServiceOperation, P]): ReservedNameServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameServiceGen[P], transformation: Transformation[P, P1]): ReservedNameServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : ReservedNameServiceGen[P]): Transformation[ReservedNameServiceOperation, P] = new Transformation[ReservedNameServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ReservedNameServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case _Set(SetInput(key, value)) => impl._set(key, value)
      case _List(ListInput(value)) => impl._list(value)
      case _Option(OptionInput(value)) => impl._option(value)
    }
  }
  case class _Set(input: SetInput) extends ReservedNameServiceOperation[SetInput, Nothing, Unit, Nothing, Nothing]
  object _Set extends Endpoint[ReservedNameServiceOperation, SetInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "_Set")
    val input: Schema[SetInput] = SetInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(smithy.api.NonEmptyString("POST"), smithy.api.NonEmptyString("/api/set/{key}"), 204),
    )
    def wrap(input: SetInput) = _Set(input)
  }
  case class _List(input: ListInput) extends ReservedNameServiceOperation[ListInput, Nothing, Unit, Nothing, Nothing]
  object _List extends Endpoint[ReservedNameServiceOperation, ListInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "_List")
    val input: Schema[ListInput] = ListInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(smithy.api.NonEmptyString("POST"), smithy.api.NonEmptyString("/api/list/{value}"), 204),
    )
    def wrap(input: ListInput) = _List(input)
  }
  case class _Option(input: OptionInput) extends ReservedNameServiceOperation[OptionInput, Nothing, Unit, Nothing, Nothing]
  object _Option extends Endpoint[ReservedNameServiceOperation, OptionInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "_Option")
    val input: Schema[OptionInput] = OptionInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(smithy.api.NonEmptyString("POST"), smithy.api.NonEmptyString("/api/option/{value}"), 204),
    )
    def wrap(input: OptionInput) = _Option(input)
  }
}

sealed trait ReservedNameServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
