package smithy4s.example.reservedNameOverride

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

trait ReservedNameOverrideServiceGen[F[_, _, _, _, _]] {
  self =>

  def setOp(set: Set): F[SetOpInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ReservedNameOverrideServiceGen[F]] = Transformation.of[ReservedNameOverrideServiceGen[F]](this)
}

object ReservedNameOverrideServiceGen extends Service.Mixin[ReservedNameOverrideServiceGen, ReservedNameOverrideServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "ReservedNameOverrideService")
  val version: String = "1.0.0"

  val hints: Hints = Hints.lazily(
    Hints(
      alloy.SimpleRestJson(),
    )
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ReservedNameOverrideServiceOperation, _, _, _, _, _]] = Vector(
    ReservedNameOverrideServiceOperation.SetOp,
  )

  def input[I, E, O, SI, SO](op: ReservedNameOverrideServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ReservedNameOverrideServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ReservedNameOverrideServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ReservedNameOverrideServiceOperation.Transformed[ReservedNameOverrideServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ReservedNameOverrideServiceGen[ReservedNameOverrideServiceOperation] = ReservedNameOverrideServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameOverrideServiceGen[P], f: PolyFunction5[P, P1]): ReservedNameOverrideServiceGen[P1] = new ReservedNameOverrideServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ReservedNameOverrideServiceOperation, P]): ReservedNameOverrideServiceGen[P] = new ReservedNameOverrideServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ReservedNameOverrideServiceGen[P]): PolyFunction5[ReservedNameOverrideServiceOperation, P] = ReservedNameOverrideServiceOperation.toPolyFunction(impl)

}

sealed trait ReservedNameOverrideServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ReservedNameOverrideServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ReservedNameOverrideServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ReservedNameOverrideServiceOperation {

  object reified extends ReservedNameOverrideServiceGen[ReservedNameOverrideServiceOperation] {
    def setOp(set: Set): SetOp = SetOp(SetOpInput(set))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ReservedNameOverrideServiceGen[P], f: PolyFunction5[P, P1]) extends ReservedNameOverrideServiceGen[P1] {
    def setOp(set: Set): P1[SetOpInput, Nothing, Unit, Nothing, Nothing] = f[SetOpInput, Nothing, Unit, Nothing, Nothing](alg.setOp(set))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ReservedNameOverrideServiceGen[P]): PolyFunction5[ReservedNameOverrideServiceOperation, P] = new PolyFunction5[ReservedNameOverrideServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ReservedNameOverrideServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class SetOp(input: SetOpInput) extends ReservedNameOverrideServiceOperation[SetOpInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameOverrideServiceGen[F]): F[SetOpInput, Nothing, Unit, Nothing, Nothing] = impl.setOp(input.set)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ReservedNameOverrideServiceOperation,SetOpInput, Nothing, Unit, Nothing, Nothing] = SetOp
  }
  object SetOp extends smithy4s.Endpoint[ReservedNameOverrideServiceOperation,SetOpInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[SetOpInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.reservedNameOverride", "SetOp"))
      .withInput(SetOpInput.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/set/"), code = 204))
    def wrap(input: SetOpInput): SetOp = SetOp(input)
  }
}

