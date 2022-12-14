package smithy4s.example

import smithy4s.Endpoint
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

  def myOp() : F[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[NameCollisionGen[F]] = new Transformation.PartiallyApplied[NameCollisionGen[F]](this)
}

object NameCollisionGen extends Service.Mixin[NameCollisionGen, NameCollisionOperation] {

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val id: ShapeId = ShapeId("smithy4s.example", "NameCollision")

  val hints : Hints = Hints.empty

  val endpoints: List[NameCollisionGen.Endpoint[_, _, _, _, _]] = List(
    MyOp,
  )

  val version: String = ""

  def endpoint[I, E, O, SI, SO](op : NameCollisionOperation[I, E, O, SI, SO]) = op.endpoint

  object reified extends NameCollisionGen[NameCollisionOperation] {
    def myOp() = MyOp()
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: NameCollisionGen[P], f: PolyFunction5[P, P1]): NameCollisionGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[NameCollisionOperation, P]): NameCollisionGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: NameCollisionGen[P], f : PolyFunction5[P, P1]) extends NameCollisionGen[P1] {
    def myOp() = f[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing](alg.myOp())
  }

  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends Transformed[NameCollisionOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]

  def toPolyFunction[P[_, _, _, _, _]](impl : NameCollisionGen[P]): PolyFunction5[NameCollisionOperation, P] = new PolyFunction5[NameCollisionOperation, P] {
    def apply[I, E, O, SI, SO](op : NameCollisionOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op.run(impl) 
  }
  case class MyOp() extends NameCollisionOperation[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: NameCollisionGen[F]): F[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing] = impl.myOp()
    def endpoint: (Unit, Endpoint[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing]) = ((), MyOp)
  }
  object MyOp extends NameCollisionGen.Endpoint[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing] with Errorable[MyOpError] {
    val id: ShapeId = ShapeId("smithy4s.example", "MyOp")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints.empty
    def wrap(input: Unit) = MyOp()
    override val errorable: Option[Errorable[MyOpError]] = Some(this)
    val error: UnionSchema[MyOpError] = MyOpError.schema
    def liftError(throwable: Throwable) : Option[MyOpError] = throwable match {
      case e: smithy4s.example.MyOpError => Some(MyOpError.MyOpErrorCase(e))
      case _ => None
    }
    def unliftError(e: MyOpError) : Throwable = e match {
      case MyOpError.MyOpErrorCase(e) => e
    }
  }
  sealed trait MyOpError extends scala.Product with scala.Serializable {
    @inline final def widen: MyOpError = this
  }
  object MyOpError extends ShapeTag.Companion[MyOpError] {
    val id: ShapeId = ShapeId("smithy4s.example", "MyOpError")

    val hints : Hints = Hints.empty

    case class MyOpErrorCase(myOpError: smithy4s.example.MyOpError) extends MyOpError

    object MyOpErrorCase {
      val hints : Hints = Hints.empty
      val schema: Schema[MyOpErrorCase] = bijection(smithy4s.example.MyOpError.schema.addHints(hints), MyOpErrorCase(_), _.myOpError)
      val alt = schema.oneOf[MyOpError]("MyOpError")
    }

    implicit val schema: UnionSchema[MyOpError] = union(
      MyOpErrorCase.alt,
    ){
      case c : MyOpErrorCase => MyOpErrorCase.alt(c)
    }
  }
}

sealed trait NameCollisionOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: NameCollisionGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[NameCollisionOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}
