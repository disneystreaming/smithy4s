package smithy4s.example.accept

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.BiFunctorAlgebra
import smithy4s.kinds.FunctorAlgebra
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait AcceptHeaderTestServiceGen[F[_, _, _, _, _]] {
  self =>

  /** Operation with no media types - should use default Accept header
    * 
    * HTTP POST /default
    */
  def defaultAcceptHeader(data: Option[String] = None): F[DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing]
  /** Operation with XML output media type
    * 
    * HTTP POST /xml-output
    */
  def xmlOutput(data: Option[String] = None): F[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing]
  /** Operation with Blob output without media type
    * 
    * HTTP POST /blob-no-media
    */
  def blobOutputNoMediaType(data: Option[String] = None): F[BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing]
  /** Operation with Blob output that has media type
    * 
    * HTTP POST /blob-with-media
    */
  def blobOutputWithMediaType(data: Option[String] = None): F[BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing]
  /** Operation with different media types for input and output
    * 
    * HTTP POST /json-xml
    * 
    * @param data
    *   JSON payload type
    */
  def jsonInputXmlOutput(data: Option[JsonPayload] = None): F[JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing]

}

object AcceptHeaderTestServiceGen extends Service.Mixin[AcceptHeaderTestServiceGen, AcceptHeaderTestServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.accept", "AcceptHeaderTestService")
  val version: String = ""

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "simpleRestJson"), smithy4s.Document.obj()),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[AcceptHeaderTestServiceOperation, _, _, _, _, _]] = Vector(
    AcceptHeaderTestServiceOperation.DefaultAcceptHeader,
    AcceptHeaderTestServiceOperation.XmlOutput,
    AcceptHeaderTestServiceOperation.BlobOutputNoMediaType,
    AcceptHeaderTestServiceOperation.BlobOutputWithMediaType,
    AcceptHeaderTestServiceOperation.JsonInputXmlOutput,
  )

  def input[I, E, O, SI, SO](op: AcceptHeaderTestServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: AcceptHeaderTestServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: AcceptHeaderTestServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends AcceptHeaderTestServiceOperation.Transformed[AcceptHeaderTestServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: AcceptHeaderTestServiceGen[AcceptHeaderTestServiceOperation] = AcceptHeaderTestServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: AcceptHeaderTestServiceGen[P], f: PolyFunction5[P, P1]): AcceptHeaderTestServiceGen[P1] = new AcceptHeaderTestServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[AcceptHeaderTestServiceOperation, P]): AcceptHeaderTestServiceGen[P] = new AcceptHeaderTestServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[P]): PolyFunction5[AcceptHeaderTestServiceOperation, P] = AcceptHeaderTestServiceOperation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[AcceptHeaderTestServiceGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[AcceptHeaderTestServiceGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[AcceptHeaderTestServiceGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[AcceptHeaderTestServiceGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: AcceptHeaderTestServiceGen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[AcceptHeaderTestServiceGen[F]] = Transformation.of(alg)
  }
}

sealed trait AcceptHeaderTestServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[AcceptHeaderTestServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object AcceptHeaderTestServiceOperation {

  object reified extends AcceptHeaderTestServiceGen[AcceptHeaderTestServiceOperation] {
    def defaultAcceptHeader(data: Option[String] = None): DefaultAcceptHeader = DefaultAcceptHeader(DefaultAcceptHeaderInput(data))
    def xmlOutput(data: Option[String] = None): XmlOutput = XmlOutput(XmlOutputInput(data))
    def blobOutputNoMediaType(data: Option[String] = None): BlobOutputNoMediaType = BlobOutputNoMediaType(BlobOutputNoMediaTypeInput(data))
    def blobOutputWithMediaType(data: Option[String] = None): BlobOutputWithMediaType = BlobOutputWithMediaType(BlobOutputWithMediaTypeInput(data))
    def jsonInputXmlOutput(data: Option[JsonPayload] = None): JsonInputXmlOutput = JsonInputXmlOutput(JsonInputXmlOutputInput(data))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: AcceptHeaderTestServiceGen[P], f: PolyFunction5[P, P1]) extends AcceptHeaderTestServiceGen[P1] {
    def defaultAcceptHeader(data: Option[String] = None): P1[DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing] = f[DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing](this.alg.defaultAcceptHeader(data))
    def xmlOutput(data: Option[String] = None): P1[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing] = f[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing](this.alg.xmlOutput(data))
    def blobOutputNoMediaType(data: Option[String] = None): P1[BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing] = f[BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing](this.alg.blobOutputNoMediaType(data))
    def blobOutputWithMediaType(data: Option[String] = None): P1[BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing] = f[BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing](this.alg.blobOutputWithMediaType(data))
    def jsonInputXmlOutput(data: Option[JsonPayload] = None): P1[JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing] = f[JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing](this.alg.jsonInputXmlOutput(data))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[P]): PolyFunction5[AcceptHeaderTestServiceOperation, P] = new PolyFunction5[AcceptHeaderTestServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: AcceptHeaderTestServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DefaultAcceptHeader(input: DefaultAcceptHeaderInput) extends AcceptHeaderTestServiceOperation[DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[F]): F[DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing] = impl.defaultAcceptHeader(input.data)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[AcceptHeaderTestServiceOperation,DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing] = DefaultAcceptHeader
  }
  object DefaultAcceptHeader extends smithy4s.Endpoint[AcceptHeaderTestServiceOperation,DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing] {
    val schema: OperationSchema[DefaultAcceptHeaderInput, Nothing, DefaultAcceptHeaderOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.accept", "DefaultAcceptHeader"))
      .withInput(DefaultAcceptHeaderInput.schema)
      .withOutput(DefaultAcceptHeaderOutput.schema)
      .withHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Operation with no media types - should use default Accept header")), Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("POST"), "uri" -> smithy4s.Document.fromString("/default"))))
    def wrap(input: DefaultAcceptHeaderInput): DefaultAcceptHeader = DefaultAcceptHeader(input)
  }
  final case class XmlOutput(input: XmlOutputInput) extends AcceptHeaderTestServiceOperation[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[F]): F[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing] = impl.xmlOutput(input.data)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[AcceptHeaderTestServiceOperation,XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing] = XmlOutput
  }
  object XmlOutput extends smithy4s.Endpoint[AcceptHeaderTestServiceOperation,XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing] {
    val schema: OperationSchema[XmlOutputInput, Nothing, XmlOutputOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.accept", "XmlOutput"))
      .withInput(XmlOutputInput.schema)
      .withOutput(XmlOutputOutput.schema)
      .withHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Operation with XML output media type")), Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("POST"), "uri" -> smithy4s.Document.fromString("/xml-output"))))
    def wrap(input: XmlOutputInput): XmlOutput = XmlOutput(input)
  }
  final case class BlobOutputNoMediaType(input: BlobOutputNoMediaTypeInput) extends AcceptHeaderTestServiceOperation[BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[F]): F[BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing] = impl.blobOutputNoMediaType(input.data)
    def ordinal: Int = 2
    def endpoint: smithy4s.Endpoint[AcceptHeaderTestServiceOperation,BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing] = BlobOutputNoMediaType
  }
  object BlobOutputNoMediaType extends smithy4s.Endpoint[AcceptHeaderTestServiceOperation,BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing] {
    val schema: OperationSchema[BlobOutputNoMediaTypeInput, Nothing, BlobOutputNoMediaTypeOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.accept", "BlobOutputNoMediaType"))
      .withInput(BlobOutputNoMediaTypeInput.schema)
      .withOutput(BlobOutputNoMediaTypeOutput.schema)
      .withHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Operation with Blob output without media type")), Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("POST"), "uri" -> smithy4s.Document.fromString("/blob-no-media"))))
    def wrap(input: BlobOutputNoMediaTypeInput): BlobOutputNoMediaType = BlobOutputNoMediaType(input)
  }
  final case class BlobOutputWithMediaType(input: BlobOutputWithMediaTypeInput) extends AcceptHeaderTestServiceOperation[BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[F]): F[BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing] = impl.blobOutputWithMediaType(input.data)
    def ordinal: Int = 3
    def endpoint: smithy4s.Endpoint[AcceptHeaderTestServiceOperation,BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing] = BlobOutputWithMediaType
  }
  object BlobOutputWithMediaType extends smithy4s.Endpoint[AcceptHeaderTestServiceOperation,BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing] {
    val schema: OperationSchema[BlobOutputWithMediaTypeInput, Nothing, BlobOutputWithMediaTypeOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.accept", "BlobOutputWithMediaType"))
      .withInput(BlobOutputWithMediaTypeInput.schema)
      .withOutput(BlobOutputWithMediaTypeOutput.schema)
      .withHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Operation with Blob output that has media type")), Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("POST"), "uri" -> smithy4s.Document.fromString("/blob-with-media"))))
    def wrap(input: BlobOutputWithMediaTypeInput): BlobOutputWithMediaType = BlobOutputWithMediaType(input)
  }
  final case class JsonInputXmlOutput(input: JsonInputXmlOutputInput) extends AcceptHeaderTestServiceOperation[JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: AcceptHeaderTestServiceGen[F]): F[JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing] = impl.jsonInputXmlOutput(input.data)
    def ordinal: Int = 4
    def endpoint: smithy4s.Endpoint[AcceptHeaderTestServiceOperation,JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing] = JsonInputXmlOutput
  }
  object JsonInputXmlOutput extends smithy4s.Endpoint[AcceptHeaderTestServiceOperation,JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing] {
    val schema: OperationSchema[JsonInputXmlOutputInput, Nothing, JsonInputXmlOutputOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.accept", "JsonInputXmlOutput"))
      .withInput(JsonInputXmlOutputInput.schema)
      .withOutput(JsonInputXmlOutputOutput.schema)
      .withHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Operation with different media types for input and output")), Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("POST"), "uri" -> smithy4s.Document.fromString("/json-xml"))))
    def wrap(input: JsonInputXmlOutputInput): JsonInputXmlOutput = JsonInputXmlOutput(input)
  }
}

