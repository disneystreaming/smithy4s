package smithy4s.example

import smithy4s.Errorable
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.UnionSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

trait NameCollisionGen[F[_, _, _, _, _]] {
  self =>

  def myOp(): F[Unit, NameCollisionOperation.MyOpError, Unit, Nothing, Nothing]
  def endpoint(): F[Unit, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[NameCollisionGen[F]] = Transformation.of[NameCollisionGen[F]](this)
}

object NameCollisionGen extends Service.Mixin[NameCollisionGen, NameCollisionOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "NameCollision")
  val version: String = ""

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[NameCollisionOperation, _, _, _, _, _]] = Vector(
    NameCollisionOperation.MyOp,
    NameCollisionOperation.Endpoint,
  )

  def input[I, E, O, SI, SO](op: NameCollisionOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: NameCollisionOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: NameCollisionOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends NameCollisionOperation.Transformed[NameCollisionOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: NameCollisionGen[NameCollisionOperation] = NameCollisionOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: NameCollisionGen[P], f: PolyFunction5[P, P1]): NameCollisionGen[P1] = new NameCollisionOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[NameCollisionOperation, P]): NameCollisionGen[P] = new NameCollisionOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: NameCollisionGen[P]): PolyFunction5[NameCollisionOperation, P] = NameCollisionOperation.toPolyFunction(impl)

  type MyOpError = NameCollisionOperation.MyOpError
  val MyOpError = NameCollisionOperation.MyOpError
}

sealed trait NameCollisionOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: NameCollisionGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: smithy4s.Endpoint[NameCollisionOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object NameCollisionOperation {

  object reified extends NameCollisionGen[NameCollisionOperation] {
    def myOp() = MyOp()
    def endpoint() = Endpoint()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: NameCollisionGen[P], f: PolyFunction5[P, P1]) extends NameCollisionGen[P1] {
    def myOp() = f[Unit, NameCollisionOperation.MyOpError, Unit, Nothing, Nothing](alg.myOp())
    def endpoint() = f[Unit, Nothing, Unit, Nothing, Nothing](alg.endpoint())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: NameCollisionGen[P]): PolyFunction5[NameCollisionOperation, P] = new PolyFunction5[NameCollisionOperation, P] {
    def apply[I, E, O, SI, SO](op: NameCollisionOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class MyOp() extends NameCollisionOperation[Unit, NameCollisionOperation.MyOpError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: NameCollisionGen[F]): F[Unit, NameCollisionOperation.MyOpError, Unit, Nothing, Nothing] = impl.myOp()
    def ordinal = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[NameCollisionOperation,Unit, NameCollisionOperation.MyOpError, Unit, Nothing, Nothing] = MyOp
  }
  object MyOp extends smithy4s.Endpoint[NameCollisionOperation,Unit, NameCollisionOperation.MyOpError, Unit, Nothing, Nothing] with Errorable[MyOpError] {
    val id: ShapeId = ShapeId("smithy4s.example", "MyOp")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: Unit) = MyOp()
    override val errorable: Option[Errorable[MyOpError]] = Some(this)
    val error: UnionSchema[MyOpError] = MyOpError.schema
    def liftError(throwable: Throwable): Option[MyOpError] = throwable match {
      case e: smithy4s.example.MyOpError => Some(MyOpError.MyOpErrorCase(e))
      case _ => None
    }
    def unliftError(e: MyOpError): Throwable = e match {
      case MyOpError.MyOpErrorCase(e) => e
    }
  }
  sealed trait MyOpError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: MyOpError = this
    def $ordinal: Int

    object project {
      def myOpError: Option[smithy4s.example.MyOpError] = MyOpError.MyOpErrorCase.alt.project.lift(self).map(_.myOpError)
    }

    def accept[A](visitor: MyOpError.Visitor[A]): A = this match {
      case value: MyOpError.MyOpErrorCase => visitor.myOpError(value.myOpError)
    }
  }
  object MyOpError extends ShapeTag.Companion[MyOpError] {

    def myOpError(myOpError: smithy4s.example.MyOpError): MyOpError = MyOpErrorCase(myOpError)

    val id: ShapeId = ShapeId("smithy4s.example", "MyOpError")

    val hints: Hints = Hints.empty

    final case class MyOpErrorCase(myOpError: smithy4s.example.MyOpError) extends MyOpError { final def $ordinal: Int = 0 }

    object MyOpErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[MyOpError.MyOpErrorCase] = bijection(smithy4s.example.MyOpError.schema.addHints(hints), MyOpError.MyOpErrorCase(_), _.myOpError)
      val alt = schema.oneOf[MyOpError]("MyOpError")
    }

    trait Visitor[A] {
      def myOpError(value: smithy4s.example.MyOpError): A
    }

    implicit val schema: UnionSchema[MyOpError] = union(
      MyOpError.MyOpErrorCase.alt,
    ){
      _.$ordinal
    }
  }
  final case class Endpoint() extends NameCollisionOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: NameCollisionGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl.endpoint()
    def ordinal = 1
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[NameCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] = Endpoint
  }
  object Endpoint extends smithy4s.Endpoint[NameCollisionOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Endpoint")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: Unit) = Endpoint()
    override val errorable: Option[Nothing] = None
  }
}

