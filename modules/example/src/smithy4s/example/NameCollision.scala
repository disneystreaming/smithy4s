package smithy4s.example

import smithy4s.Errorable
import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.Transformation
import smithy4s.Monadic
import smithy4s.Service
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.UnionSchema
import smithy4s.Hints
import smithy4s.StreamingSchema
import smithy4s.ShapeId
import smithy4s.Endpoint

trait NameCollisionGen[F[_, _, _, _, _]] {
  self =>

  def myOp() : F[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[NameCollisionGen, F] = new Transformation.PartiallyApplied[NameCollisionGen, F](this)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends NameCollisionGen[G] {
    def myOp() = transformation[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing](self.myOp())
  }
}

object NameCollisionGen extends Service[NameCollisionGen, NameCollisionOperation] {

  def apply[F[_]](implicit F: Monadic[NameCollisionGen, F]): F.type = F

  val id: ShapeId = ShapeId("smithy4s.example", "NameCollision")

  val hints : Hints = Hints.empty

  val endpoints: List[Endpoint[NameCollisionOperation, _, _, _, _, _]] = List(
    MyOp,
  )

  val version: String = ""

  def endpoint[I, E, O, SI, SO](op : NameCollisionOperation[I, E, O, SI, SO]) = op match {
    case MyOp() => ((), MyOp)
  }

  object reified extends NameCollisionGen[NameCollisionOperation] {
    def myOp() = MyOp()
  }

  def transform[P[_, _, _, _, _]](transformation: Transformation[NameCollisionOperation, P]): NameCollisionGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: NameCollisionGen[P], transformation: Transformation[P, P1]): NameCollisionGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : NameCollisionGen[P]): Transformation[NameCollisionOperation, P] = new Transformation[NameCollisionOperation, P] {
    def apply[I, E, O, SI, SO](op : NameCollisionOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case MyOp() => impl.myOp()
    }
  }
  case class MyOp() extends NameCollisionOperation[Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing]
  object MyOp extends Endpoint[NameCollisionOperation, Unit, NameCollisionGen.MyOpError, Unit, Nothing, Nothing] with Errorable[MyOpError] {
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

sealed trait NameCollisionOperation[Input, Err, Output, StreamedInput, StreamedOutput]
