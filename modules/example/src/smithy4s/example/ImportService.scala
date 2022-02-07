package smithy4s.example

import smithy4s.example.import_test.OpOutput
import smithy4s.syntax._

trait ImportServiceGen[F[_, _, _, _, _]] {
  self =>

  def importOperation() : F[Unit, Nothing, OpOutput, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) : ImportServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) extends ImportServiceGen[G] {
    def importOperation() = transformation[Unit, Nothing, OpOutput, Nothing, Nothing](self.importOperation())
  }
}

object ImportServiceGen extends smithy4s.Service[ImportServiceGen, ImportServiceOperation] {

  def apply[F[_]](implicit F: smithy4s.Monadic[ImportServiceGen, F]): F.type = F

  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ImportService")

  val hints : smithy4s.Hints = smithy4s.Hints(
    id,
    smithy4s.api.SimpleRestJson(),
  )

  val endpoints = List(
    ImportOperation,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : ImportServiceOperation[I, E, O, SI, SO]) = op match {
    case ImportOperation() => ((), ImportOperation)
  }

  object reified extends ImportServiceGen[ImportServiceOperation] {
    def importOperation() = ImportOperation()
  }

  def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[ImportServiceOperation, P]): ImportServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ImportServiceGen[P], transformation: smithy4s.Transformation[P, P1]): ImportServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : ImportServiceGen[P]): smithy4s.Transformation[ImportServiceOperation, P] = new smithy4s.Transformation[ImportServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : ImportServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case ImportOperation() => impl.importOperation()
    }
  }
  case class ImportOperation() extends ImportServiceOperation[Unit, Nothing, OpOutput, Nothing, Nothing]
  object ImportOperation extends smithy4s.Endpoint[ImportServiceOperation, Unit, Nothing, OpOutput, Nothing, Nothing] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example.import_test", "ImportOperation")
    val input: smithy4s.Schema[Unit] = unit.withHints(smithy4s.internals.InputOutput.Input)
    val output: smithy4s.Schema[OpOutput] = OpOutput.schema.withHints(smithy4s.internals.InputOutput.Output)
    val streamedInput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val streamedOutput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
      smithy.api.Http(smithy.api.NonEmptyString("GET"), smithy.api.NonEmptyString("/test"), Some(200)),
    )
    def wrap(input: Unit) = ImportOperation()
  }
}

sealed trait ImportServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
