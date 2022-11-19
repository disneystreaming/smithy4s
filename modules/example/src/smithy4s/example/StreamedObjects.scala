package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.kinds.PolyFunction5
import smithy4s.Transformation
import smithy4s.kinds.FunctorAlgebra
import smithy4s.ShapeId
import smithy4s.Service
import smithy4s.kinds.BiFunctorAlgebra
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.Hints
import smithy4s.StreamingSchema

trait StreamedObjectsGen[F[_, _, _, _, _]] {
  self =>

  def putStreamedObject(key: String) : F[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing]
  def getStreamedObject(key: String) : F[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob]

  def transform : Transformation.PartiallyApplied[StreamedObjectsGen[F]] = new Transformation.PartiallyApplied[StreamedObjectsGen[F]](this)
}

object StreamedObjectsGen extends Service.Mixin[StreamedObjectsGen, StreamedObjectsOperation] {

  def apply[F[_]](implicit F: FunctorAlgebra[StreamedObjectsGen, F]): F.type = F

  type WithError[F[_, _]] = BiFunctorAlgebra[StreamedObjectsGen, F]
  object WithError {
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val id: ShapeId = ShapeId("smithy4s.example", "StreamedObjects")

  val hints : Hints = Hints.empty

  val endpoints: List[StreamedObjectsGen.Endpoint[_, _, _, _, _]] = List(
    PutStreamedObject,
    GetStreamedObject,
  )

  val version: String = "1.0.0"

  def endpoint[I, E, O, SI, SO](op : StreamedObjectsOperation[I, E, O, SI, SO]) = op match {
    case PutStreamedObject(input) => (input, PutStreamedObject)
    case GetStreamedObject(input) => (input, GetStreamedObject)
  }

  object reified extends StreamedObjectsGen[StreamedObjectsOperation] {
    def putStreamedObject(key: String) = PutStreamedObject(PutStreamedObjectInput(key))
    def getStreamedObject(key: String) = GetStreamedObject(GetStreamedObjectInput(key))
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: StreamedObjectsGen[P], f: PolyFunction5[P, P1]): StreamedObjectsGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[StreamedObjectsOperation, P]): StreamedObjectsGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: StreamedObjectsGen[P], f : PolyFunction5[P, P1]) extends StreamedObjectsGen[P1] {
    def putStreamedObject(key: String) = f[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing](alg.putStreamedObject(key))
    def getStreamedObject(key: String) = f[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob](alg.getStreamedObject(key))
  }

  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends Transformed[StreamedObjectsOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]

  def toPolyFunction[P[_, _, _, _, _]](impl : StreamedObjectsGen[P]): PolyFunction5[StreamedObjectsOperation, P] = new PolyFunction5[StreamedObjectsOperation, P] {
    def apply[I, E, O, SI, SO](op : StreamedObjectsOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case PutStreamedObject(PutStreamedObjectInput(key)) => impl.putStreamedObject(key)
      case GetStreamedObject(GetStreamedObjectInput(key)) => impl.getStreamedObject(key)
    }
  }
  case class PutStreamedObject(input: PutStreamedObjectInput) extends StreamedObjectsOperation[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing]
  object PutStreamedObject extends StreamedObjectsGen.Endpoint[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "PutStreamedObject")
    val input: Schema[PutStreamedObjectInput] = PutStreamedObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[StreamedBlob] = StreamingSchema("PutStreamedObjectInput", StreamedBlob.schema.addHints(smithy.api.Default(smithy4s.Document.fromString(""))))
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints.empty
    def wrap(input: PutStreamedObjectInput) = PutStreamedObject(input)
  }
  case class GetStreamedObject(input: GetStreamedObjectInput) extends StreamedObjectsOperation[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob]
  object GetStreamedObject extends StreamedObjectsGen.Endpoint[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObject")
    val input: Schema[GetStreamedObjectInput] = GetStreamedObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetStreamedObjectOutput] = GetStreamedObjectOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[StreamedBlob] = StreamingSchema("GetStreamedObjectOutput", StreamedBlob.schema.addHints(smithy.api.Default(smithy4s.Document.fromString(""))))
    val hints : Hints = Hints.empty
    def wrap(input: GetStreamedObjectInput) = GetStreamedObject(input)
  }
}

sealed trait StreamedObjectsOperation[Input, Err, Output, StreamedInput, StreamedOutput]
