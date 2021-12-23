package smithy4s.example

import smithy4s.http
import smithy4s.syntax._

trait FooServiceGen[F[_, _, _, _, _]] {
  self =>

  def getFoo() : F[Unit, Nothing, GetFooOutput, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) : FooServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) extends FooServiceGen[G] {
    def getFoo() = transformation[Unit, Nothing, GetFooOutput, Nothing, Nothing](self.getFoo())
  }
}

object FooServiceGen extends smithy4s.Service[FooServiceGen, FooServiceOperation] {

  val hints : smithy4s.Hints = smithy4s.Hints()

  val endpoints = List(
    GetFoo,
  )

  def namespace: String = "smithy4s.example"
  val name: String = "FooService"
  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : FooServiceOperation[I, E, O, SI, SO]) = op match {
    case GetFoo() => ((), GetFoo)
  }

  object reified extends FooServiceGen[FooServiceOperation] {
    def getFoo() = GetFoo()
  }

  def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[FooServiceOperation, P]): FooServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: FooServiceGen[P], transformation: smithy4s.Transformation[P, P1]): FooServiceGen[P1] = alg.transform(transformation)

  def asTransformationGen[P[_, _, _, _, _]](impl : FooServiceGen[P]): smithy4s.Transformation[FooServiceOperation, P] = new smithy4s.Transformation[FooServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : FooServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case GetFoo() => impl.getFoo()
    }
  }
  case class GetFoo() extends FooServiceOperation[Unit, Nothing, GetFooOutput, Nothing, Nothing]
  object GetFoo extends smithy4s.Endpoint[FooServiceOperation, Unit, Nothing, GetFooOutput, Nothing, Nothing] with http.HttpEndpoint[Unit] {
    def name: String = "GetFoo"
    val input: smithy4s.Schema[Unit] = unit.withHints(smithy4s.internals.InputOutput.Input)
    val output: smithy4s.Schema[GetFooOutput] = GetFooOutput.schema.withHints(smithy4s.internals.InputOutput.Output)
    val streamedInput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val streamedOutput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val hints : smithy4s.Hints = smithy4s.Hints(
      smithy.api.Http(smithy.api.NonEmptyString("GET"), smithy.api.NonEmptyString("/foo"), Some(200)),
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = GetFoo()
    def path(input: Unit) = s"foo"
    val path = List(http.PathSegment.static("foo"))
    val code: Int = 200
    val method: http.HttpMethod = http.HttpMethod.GET
  }
}

sealed trait FooServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
