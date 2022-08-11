package smithy4s.example

import smithy4s.schema.Schema._

trait StreamedObjectsGen[F[_, _, _, _, _]] {
  self =>

  def putStreamedObject(key: String) : F[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing]
  def getStreamedObject(key: String) : F[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob]

  def transform[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) : StreamedObjectsGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) extends StreamedObjectsGen[G] {
    def putStreamedObject(key: String) = transformation[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing](self.putStreamedObject(key))
    def getStreamedObject(key: String) = transformation[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob](self.getStreamedObject(key))
  }
}

object StreamedObjectsGen extends smithy4s.Service[StreamedObjectsGen, StreamedObjectsOperation] {

  def apply[F[_]](implicit F: smithy4s.Monadic[StreamedObjectsGen, F]): F.type = F

  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "StreamedObjects")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  val endpoints: List[smithy4s.Endpoint[StreamedObjectsOperation, _, _, _, _, _]] = List(
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

  def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[StreamedObjectsOperation, P]): StreamedObjectsGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: StreamedObjectsGen[P], transformation: smithy4s.Transformation[P, P1]): StreamedObjectsGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : StreamedObjectsGen[P]): smithy4s.Transformation[StreamedObjectsOperation, P] = new smithy4s.Transformation[StreamedObjectsOperation, P] {
    def apply[I, E, O, SI, SO](op : StreamedObjectsOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case PutStreamedObject(PutStreamedObjectInput(key)) => impl.putStreamedObject(key)
      case GetStreamedObject(GetStreamedObjectInput(key)) => impl.getStreamedObject(key)
    }
  }
  case class PutStreamedObject(input: PutStreamedObjectInput) extends StreamedObjectsOperation[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing]
  object PutStreamedObject extends smithy4s.Endpoint[StreamedObjectsOperation, PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PutStreamedObject")
    val input: smithy4s.Schema[PutStreamedObjectInput] = PutStreamedObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: smithy4s.Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : smithy4s.StreamingSchema[StreamedBlob] = smithy4s.StreamingSchema("PutStreamedObjectInput", StreamedBlob.schema.addHints(smithy.api.Default(smithy4s.Document.fromString(""))))
    val streamedOutput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    def wrap(input: PutStreamedObjectInput) = PutStreamedObject(input)
  }
  case class GetStreamedObject(input: GetStreamedObjectInput) extends StreamedObjectsOperation[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob]
  object GetStreamedObject extends smithy4s.Endpoint[StreamedObjectsOperation, GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetStreamedObject")
    val input: smithy4s.Schema[GetStreamedObjectInput] = GetStreamedObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: smithy4s.Schema[GetStreamedObjectOutput] = GetStreamedObjectOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val streamedOutput : smithy4s.StreamingSchema[StreamedBlob] = smithy4s.StreamingSchema("GetStreamedObjectOutput", StreamedBlob.schema.addHints(smithy.api.Default(smithy4s.Document.fromString(""))))
    val hints : smithy4s.Hints = smithy4s.Hints.empty
    def wrap(input: GetStreamedObjectInput) = GetStreamedObject(input)
  }
}

sealed trait StreamedObjectsOperation[Input, Err, Output, StreamedInput, StreamedOutput]
