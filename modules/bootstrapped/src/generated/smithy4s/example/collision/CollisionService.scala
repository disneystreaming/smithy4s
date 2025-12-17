package smithy4s.example.collision

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

trait CollisionServiceGen[F[_, _, _, _, _]] {
  self =>

  def algParameterOperation(alg: smithy4s.example.collision.String): F[AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[CollisionServiceGen[F]] = Transformation.of[CollisionServiceGen[F]](this)
}

object CollisionServiceGen extends Service.Mixin[CollisionServiceGen, CollisionServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.collision", "CollisionService")
  val version: java.lang.String = ""

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[CollisionServiceOperation, _, _, _, _, _]] = Vector(
    CollisionServiceOperation.AlgParameterOperation,
  )

  def input[I, E, O, SI, SO](op: CollisionServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: CollisionServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: CollisionServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends CollisionServiceOperation.Transformed[CollisionServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: CollisionServiceGen[CollisionServiceOperation] = CollisionServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: CollisionServiceGen[P], f: PolyFunction5[P, P1]): CollisionServiceGen[P1] = new CollisionServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[CollisionServiceOperation, P]): CollisionServiceGen[P] = new CollisionServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: CollisionServiceGen[P]): PolyFunction5[CollisionServiceOperation, P] = CollisionServiceOperation.toPolyFunction(impl)

}

sealed trait CollisionServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: CollisionServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[CollisionServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object CollisionServiceOperation {

  object reified extends CollisionServiceGen[CollisionServiceOperation] {
    def algParameterOperation(alg: smithy4s.example.collision.String): AlgParameterOperation = AlgParameterOperation(AlgParameterOperationInput(alg))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: CollisionServiceGen[P], f: PolyFunction5[P, P1]) extends CollisionServiceGen[P1] {
    def algParameterOperation(alg: smithy4s.example.collision.String): P1[AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing] = f[AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing](this.alg.algParameterOperation(alg))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: CollisionServiceGen[P]): PolyFunction5[CollisionServiceOperation, P] = new PolyFunction5[CollisionServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: CollisionServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class AlgParameterOperation(input: AlgParameterOperationInput) extends CollisionServiceOperation[AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CollisionServiceGen[F]): F[AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing] = impl.algParameterOperation(input.alg)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[CollisionServiceOperation,AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing] = AlgParameterOperation
  }
  object AlgParameterOperation extends smithy4s.Endpoint[CollisionServiceOperation,AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[AlgParameterOperationInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.collision", "AlgParameterOperation"))
      .withInput(AlgParameterOperationInput.schema)
      .withOutput(unit)
    def wrap(input: AlgParameterOperationInput): AlgParameterOperation = AlgParameterOperation(input)
  }
}

