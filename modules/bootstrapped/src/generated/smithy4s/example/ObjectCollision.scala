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

trait ObjectCollisionGen[F[_, _, _, _, _]] {
  self =>

  def _clone(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _equals(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _finalize(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _getClass(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _hashCode(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _notify(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _notifyAll(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _toString(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def _wait(): F[Unit, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ObjectCollisionGen[F]] = Transformation.of[ObjectCollisionGen[F]](this)
}

object ObjectCollisionGen extends Service.Mixin[ObjectCollisionGen, ObjectCollisionOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ObjectCollision")
  val version: String = ""

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ObjectCollisionOperation, _, _, _, _, _]] = Vector(
    ObjectCollisionOperation.Clone,
    ObjectCollisionOperation.Equals,
    ObjectCollisionOperation.Finalize,
    ObjectCollisionOperation.GetClass,
    ObjectCollisionOperation.HashCode,
    ObjectCollisionOperation.Notify,
    ObjectCollisionOperation.NotifyAll,
    ObjectCollisionOperation.ToString,
    ObjectCollisionOperation.Wait,
  )

  def input[I, E, O, SI, SO](op: ObjectCollisionOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ObjectCollisionOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ObjectCollisionOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ObjectCollisionOperation.Transformed[ObjectCollisionOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ObjectCollisionGen[ObjectCollisionOperation] = ObjectCollisionOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ObjectCollisionGen[P], f: PolyFunction5[P, P1]): ObjectCollisionGen[P1] = new ObjectCollisionOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ObjectCollisionOperation, P]): ObjectCollisionGen[P] = new ObjectCollisionOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectCollisionGen[P]): PolyFunction5[ObjectCollisionOperation, P] = ObjectCollisionOperation.toPolyFunction(impl)

}

sealed trait ObjectCollisionOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ObjectCollisionOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ObjectCollisionOperation {

  object reified extends ObjectCollisionGen[ObjectCollisionOperation] {
    def _clone() = Clone()
    def _equals() = Equals()
    def _finalize() = Finalize()
    def _getClass() = GetClass()
    def _hashCode() = HashCode()
    def _notify() = Notify()
    def _notifyAll() = NotifyAll()
    def _toString() = ToString()
    def _wait() = Wait()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ObjectCollisionGen[P], f: PolyFunction5[P, P1]) extends ObjectCollisionGen[P1] {
    def _clone() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._clone())
    def _equals() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._equals())
    def _finalize() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._finalize())
    def _getClass() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._getClass())
    def _hashCode() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._hashCode())
    def _notify() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._notify())
    def _notifyAll() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._notifyAll())
    def _toString() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._toString())
    def _wait() = f[Unit, Nothing, Unit, Nothing, Nothing](alg._wait())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ObjectCollisionGen[P]): PolyFunction5[ObjectCollisionOperation, P] = new PolyFunction5[ObjectCollisionOperation, P] {
    def apply[I, E, O, SI, SO](op: ObjectCollisionOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Clone() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._clone()
    def ordinal = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = Clone
  }
  object Clone extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Clone"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = Clone()
  }
  final case class Equals() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._equals()
    def ordinal = 1
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = Equals
  }
  object Equals extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Equals"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = Equals()
  }
  final case class Finalize() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._finalize()
    def ordinal = 2
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = Finalize
  }
  object Finalize extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Finalize"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = Finalize()
  }
  final case class GetClass() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._getClass()
    def ordinal = 3
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = GetClass
  }
  object GetClass extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetClass"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = GetClass()
  }
  final case class HashCode() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._hashCode()
    def ordinal = 4
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = HashCode
  }
  object HashCode extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "HashCode"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = HashCode()
  }
  final case class Notify() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._notify()
    def ordinal = 5
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = Notify
  }
  object Notify extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Notify"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = Notify()
  }
  final case class NotifyAll() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._notifyAll()
    def ordinal = 6
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = NotifyAll
  }
  object NotifyAll extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "NotifyAll"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = NotifyAll()
  }
  final case class ToString() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._toString()
    def ordinal = 7
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = ToString
  }
  object ToString extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "ToString"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = ToString()
  }
  final case class Wait() extends ObjectCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ObjectCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl._wait()
    def ordinal = 8
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = Wait
  }
  object Wait extends smithy4s.Endpoint[ObjectCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Wait"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = Wait()
  }
}

