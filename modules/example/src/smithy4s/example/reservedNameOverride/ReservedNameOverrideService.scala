package smithy4s.example.reservedNameOverride

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.Transformation
import smithy4s.Monadic
import smithy4s.Service
import smithy4s.Hints
import smithy4s.StreamingSchema
import smithy4s.ShapeId
import smithy4s.Endpoint

trait ReservedNameOverrideServiceGen[F[_, _, _, _, _]] {
  self =>

  def setOp(set: _Set) : F[SetOpInput, Nothing, Unit, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : Transformation[F, G]) : ReservedNameOverrideServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends ReservedNameOverrideServiceGen[G] {
    def setOp(set: _Set) = transformation[SetOpInput, Nothing, Unit, Nothing, Nothing](self.setOp(set))
  }
}

object ReservedNameOverrideServiceGen extends Service[ReservedNameOverrideServiceGen, ReservedNameOverrideServiceOperation] {

  def apply[F[_]](implicit F: Monadic[ReservedNameOverrideServiceGen, F]): F.type = F

  val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "ReservedNameOverrideService")

  val hints : Hints = Hints(
    smithy4s.api.SimpleRestJson(),
  )

  val endpoints: List[Endpoint[ReservedNameOverrideServiceOperation, _, _, _, _, _]] = List(
    SetOp,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : ReservedNameOverrideServiceOperation[I, E, O, SI, SO]) = op match {
    case SetOp(input) => (input, SetOp)
  }

  object reified extends ReservedNameOverrideServiceGen[ReservedNameOverrideServiceOperation] {
    def setOp(set: _Set) = SetOp(SetOpInput(set))
  }

  def transform[P[_, _, _, _, _]](transformation: Transformation[ReservedNameOverrideServiceOperation, P]): ReservedNameOverrideServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameOverrideServiceGen[P], transformation: Transformation[P, P1]): ReservedNameOverrideServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : ReservedNameOverrideServiceGen[P]): Transformation[ReservedNameOverrideServiceOperation, P] = new Transformation[ReservedNameOverrideServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ReservedNameOverrideServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case SetOp(SetOpInput(set)) => impl.setOp(set)
    }
  }
  case class SetOp(input: SetOpInput) extends ReservedNameOverrideServiceOperation[SetOpInput, Nothing, Unit, Nothing, Nothing]
  object SetOp extends Endpoint[ReservedNameOverrideServiceOperation, SetOpInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "SetOp")
    val input: Schema[SetOpInput] = SetOpInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/set/"), code = 204),
    )
    def wrap(input: SetOpInput) = SetOp(input)
  }
}

sealed trait ReservedNameOverrideServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
