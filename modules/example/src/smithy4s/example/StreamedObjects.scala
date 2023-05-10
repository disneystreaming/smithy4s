package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.unit

trait StreamedObjectsGen[F[_, _, _, _, _]] {
  self =>

  def putStreamedObject(key: String): F[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing]
  def getStreamedObject(key: String): F[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob]

  def transform: Transformation.PartiallyApplied[StreamedObjectsGen[F]] = Transformation.of[StreamedObjectsGen[F]](this)
}

object StreamedObjectsGen extends Service.Mixin[StreamedObjectsGen, StreamedObjectsOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "StreamedObjects")
  val version: String = "1.0.0"

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: List[smithy4s.Endpoint[StreamedObjectsOperation, _, _, _, _, _]] = List(
    StreamedObjectsOperation.PutStreamedObject,
    StreamedObjectsOperation.GetStreamedObject,
  )

  def endpoint[I, E, O, SI, SO](op: StreamedObjectsOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends StreamedObjectsOperation.Transformed[StreamedObjectsOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: StreamedObjectsGen[StreamedObjectsOperation] = StreamedObjectsOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: StreamedObjectsGen[P], f: PolyFunction5[P, P1]): StreamedObjectsGen[P1] = new StreamedObjectsOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[StreamedObjectsOperation, P]): StreamedObjectsGen[P] = new StreamedObjectsOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: StreamedObjectsGen[P]): PolyFunction5[StreamedObjectsOperation, P] = StreamedObjectsOperation.toPolyFunction(impl)

}

sealed trait StreamedObjectsOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: StreamedObjectsGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[StreamedObjectsOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}

object StreamedObjectsOperation {

  object reified extends StreamedObjectsGen[StreamedObjectsOperation] {
    def putStreamedObject(key: String) = PutStreamedObject(PutStreamedObjectInput(key))
    def getStreamedObject(key: String) = GetStreamedObject(GetStreamedObjectInput(key))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: StreamedObjectsGen[P], f: PolyFunction5[P, P1]) extends StreamedObjectsGen[P1] {
    def putStreamedObject(key: String) = f[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing](alg.putStreamedObject(key))
    def getStreamedObject(key: String) = f[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob](alg.getStreamedObject(key))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: StreamedObjectsGen[P]): PolyFunction5[StreamedObjectsOperation, P] = new PolyFunction5[StreamedObjectsOperation, P] {
    def apply[I, E, O, SI, SO](op: StreamedObjectsOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class PutStreamedObject(input: PutStreamedObjectInput) extends StreamedObjectsOperation[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing] {
    def run[F[_, _, _, _, _]](impl: StreamedObjectsGen[F]): F[PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing] = impl.putStreamedObject(input.key)
    def endpoint: (PutStreamedObjectInput, smithy4s.Endpoint[StreamedObjectsOperation,PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing]) = (input, PutStreamedObject)
  }
  object PutStreamedObject extends smithy4s.Endpoint[StreamedObjectsOperation,PutStreamedObjectInput, Nothing, Unit, StreamedBlob, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "PutStreamedObject")
    val input: Schema[PutStreamedObjectInput] = PutStreamedObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[StreamedBlob] = StreamingSchema("PutStreamedObjectInput", StreamedBlob.schema.addHints(smithy.api.Default(smithy4s.Document.fromString(""))))
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: PutStreamedObjectInput) = PutStreamedObject(input)
    override val errorable: Option[Nothing] = None
  }
  final case class GetStreamedObject(input: GetStreamedObjectInput) extends StreamedObjectsOperation[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob] {
    def run[F[_, _, _, _, _]](impl: StreamedObjectsGen[F]): F[GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob] = impl.getStreamedObject(input.key)
    def endpoint: (GetStreamedObjectInput, smithy4s.Endpoint[StreamedObjectsOperation,GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob]) = (input, GetStreamedObject)
  }
  object GetStreamedObject extends smithy4s.Endpoint[StreamedObjectsOperation,GetStreamedObjectInput, Nothing, GetStreamedObjectOutput, Nothing, StreamedBlob] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObject")
    val input: Schema[GetStreamedObjectInput] = GetStreamedObjectInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetStreamedObjectOutput] = GetStreamedObjectOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[StreamedBlob] = StreamingSchema("GetStreamedObjectOutput", StreamedBlob.schema.addHints(smithy.api.Default(smithy4s.Document.fromString(""))))
    val hints: Hints = Hints.empty
    def wrap(input: GetStreamedObjectInput) = GetStreamedObject(input)
    override val errorable: Option[Nothing] = None
  }
}
