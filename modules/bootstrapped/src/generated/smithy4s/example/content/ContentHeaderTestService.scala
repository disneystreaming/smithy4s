package smithy4s.example.content

import smithy4s.Blob
import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait ContentHeaderTestServiceGen[F[_, _, _, _, _]] {
  self =>

  /** Operation with empty struct - should have Content-Type when writeEmptyStructs=true, none when false
    * 
    * HTTP POST /empty-struct
    */
  def emptyStructOperation(): F[EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing]
  /** Operation with Blob input that has media type
    * 
    * HTTP POST /blob-with-media
    * 
    * @param image
    *   PNG image blob type
    */
  def blobInputWithMediaType(image: PngImage): F[BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing]
  /** Operation with no media types - should use default Content-Type header
    * 
    * HTTP POST /default
    */
  def defaultContentHeader(data: String): F[DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing]
  /** Operation with Blob input without media type
    * 
    * HTTP POST /blob-no-media
    */
  def blobInputNoMediaType(image: Blob): F[BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing]
  /** Operation with only metadata (no body) - should not have Content-Type header
    * 
    * HTTP GET /no-body
    */
  def noBodyOperation(query: Option[String] = None): F[NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing]
  /** Operation with different media types for input and output
    * 
    * HTTP POST /xml-json
    * 
    * @param data
    *   XML payload type
    */
  def xmlInputJsonOutput(data: XmlPayload): F[XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing]
  /** Operation with XML input media type
    * 
    * HTTP POST /xml-input
    * 
    * @param data
    *   XML payload type
    */
  def xmlInput(data: XmlPayload): F[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing]
  /** Operation with explicit Content-Type header - should override default behavior
    * 
    * HTTP POST /explicit-content-type
    */
  def explicitContentTypeHeader(data: Blob, contentType: Option[String] = None): F[ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[ContentHeaderTestServiceGen[F]] = Transformation.of[ContentHeaderTestServiceGen[F]](this)
}

object ContentHeaderTestServiceGen extends Service.Mixin[ContentHeaderTestServiceGen, ContentHeaderTestServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.content", "ContentHeaderTestService")
  val version: String = ""

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ContentHeaderTestServiceOperation, _, _, _, _, _]] = Vector(
    ContentHeaderTestServiceOperation.EmptyStructOperation,
    ContentHeaderTestServiceOperation.BlobInputWithMediaType,
    ContentHeaderTestServiceOperation.DefaultContentHeader,
    ContentHeaderTestServiceOperation.BlobInputNoMediaType,
    ContentHeaderTestServiceOperation.NoBodyOperation,
    ContentHeaderTestServiceOperation.XmlInputJsonOutput,
    ContentHeaderTestServiceOperation.XmlInput,
    ContentHeaderTestServiceOperation.ExplicitContentTypeHeader,
  )

  def input[I, E, O, SI, SO](op: ContentHeaderTestServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ContentHeaderTestServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ContentHeaderTestServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ContentHeaderTestServiceOperation.Transformed[ContentHeaderTestServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ContentHeaderTestServiceGen[ContentHeaderTestServiceOperation] = ContentHeaderTestServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ContentHeaderTestServiceGen[P], f: PolyFunction5[P, P1]): ContentHeaderTestServiceGen[P1] = new ContentHeaderTestServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ContentHeaderTestServiceOperation, P]): ContentHeaderTestServiceGen[P] = new ContentHeaderTestServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[P]): PolyFunction5[ContentHeaderTestServiceOperation, P] = ContentHeaderTestServiceOperation.toPolyFunction(impl)

}

sealed trait ContentHeaderTestServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ContentHeaderTestServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ContentHeaderTestServiceOperation {

  object reified extends ContentHeaderTestServiceGen[ContentHeaderTestServiceOperation] {
    def emptyStructOperation(): EmptyStructOperation = EmptyStructOperation(EmptyStructOperationInput())
    def blobInputWithMediaType(image: PngImage): BlobInputWithMediaType = BlobInputWithMediaType(BlobInputWithMediaTypeInput(image))
    def defaultContentHeader(data: String): DefaultContentHeader = DefaultContentHeader(DefaultContentHeaderInput(data))
    def blobInputNoMediaType(image: Blob): BlobInputNoMediaType = BlobInputNoMediaType(BlobInputNoMediaTypeInput(image))
    def noBodyOperation(query: Option[String] = None): NoBodyOperation = NoBodyOperation(NoBodyOperationInput(query))
    def xmlInputJsonOutput(data: XmlPayload): XmlInputJsonOutput = XmlInputJsonOutput(XmlInputJsonOutputInput(data))
    def xmlInput(data: XmlPayload): XmlInput = XmlInput(XmlInputInput(data))
    def explicitContentTypeHeader(data: Blob, contentType: Option[String] = None): ExplicitContentTypeHeader = ExplicitContentTypeHeader(ExplicitContentTypeHeaderInput(data, contentType))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ContentHeaderTestServiceGen[P], f: PolyFunction5[P, P1]) extends ContentHeaderTestServiceGen[P1] {
    def emptyStructOperation(): P1[EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing] = f[EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing](this.alg.emptyStructOperation())
    def blobInputWithMediaType(image: PngImage): P1[BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing] = f[BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing](this.alg.blobInputWithMediaType(image))
    def defaultContentHeader(data: String): P1[DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing] = f[DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing](this.alg.defaultContentHeader(data))
    def blobInputNoMediaType(image: Blob): P1[BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing] = f[BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing](this.alg.blobInputNoMediaType(image))
    def noBodyOperation(query: Option[String] = None): P1[NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing] = f[NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing](this.alg.noBodyOperation(query))
    def xmlInputJsonOutput(data: XmlPayload): P1[XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing] = f[XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing](this.alg.xmlInputJsonOutput(data))
    def xmlInput(data: XmlPayload): P1[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing] = f[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing](this.alg.xmlInput(data))
    def explicitContentTypeHeader(data: Blob, contentType: Option[String] = None): P1[ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing] = f[ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing](this.alg.explicitContentTypeHeader(data, contentType))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[P]): PolyFunction5[ContentHeaderTestServiceOperation, P] = new PolyFunction5[ContentHeaderTestServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ContentHeaderTestServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class EmptyStructOperation(input: EmptyStructOperationInput) extends ContentHeaderTestServiceOperation[EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing] = impl.emptyStructOperation()
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing] = EmptyStructOperation
  }
  object EmptyStructOperation extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing] {
    val schema: OperationSchema[EmptyStructOperationInput, Nothing, EmptyStructOperationOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "EmptyStructOperation"))
      .withInput(EmptyStructOperationInput.schema)
      .withOutput(EmptyStructOperationOutput.schema)
      .withHints(smithy.api.Documentation("Operation with empty struct - should have Content-Type when writeEmptyStructs=true, none when false"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/empty-struct"), code = 200))
    def wrap(input: EmptyStructOperationInput): EmptyStructOperation = EmptyStructOperation(input)
  }
  final case class BlobInputWithMediaType(input: BlobInputWithMediaTypeInput) extends ContentHeaderTestServiceOperation[BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing] = impl.blobInputWithMediaType(input.image)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing] = BlobInputWithMediaType
  }
  object BlobInputWithMediaType extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing] {
    val schema: OperationSchema[BlobInputWithMediaTypeInput, Nothing, BlobInputWithMediaTypeOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "BlobInputWithMediaType"))
      .withInput(BlobInputWithMediaTypeInput.schema)
      .withOutput(BlobInputWithMediaTypeOutput.schema)
      .withHints(smithy.api.Documentation("Operation with Blob input that has media type"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/blob-with-media"), code = 200))
    def wrap(input: BlobInputWithMediaTypeInput): BlobInputWithMediaType = BlobInputWithMediaType(input)
  }
  final case class DefaultContentHeader(input: DefaultContentHeaderInput) extends ContentHeaderTestServiceOperation[DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing] = impl.defaultContentHeader(input.data)
    def ordinal: Int = 2
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing] = DefaultContentHeader
  }
  object DefaultContentHeader extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing] {
    val schema: OperationSchema[DefaultContentHeaderInput, Nothing, DefaultContentHeaderOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "DefaultContentHeader"))
      .withInput(DefaultContentHeaderInput.schema)
      .withOutput(DefaultContentHeaderOutput.schema)
      .withHints(smithy.api.Documentation("Operation with no media types - should use default Content-Type header"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/default"), code = 200))
    def wrap(input: DefaultContentHeaderInput): DefaultContentHeader = DefaultContentHeader(input)
  }
  final case class BlobInputNoMediaType(input: BlobInputNoMediaTypeInput) extends ContentHeaderTestServiceOperation[BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing] = impl.blobInputNoMediaType(input.image)
    def ordinal: Int = 3
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing] = BlobInputNoMediaType
  }
  object BlobInputNoMediaType extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing] {
    val schema: OperationSchema[BlobInputNoMediaTypeInput, Nothing, BlobInputNoMediaTypeOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "BlobInputNoMediaType"))
      .withInput(BlobInputNoMediaTypeInput.schema)
      .withOutput(BlobInputNoMediaTypeOutput.schema)
      .withHints(smithy.api.Documentation("Operation with Blob input without media type"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/blob-no-media"), code = 200))
    def wrap(input: BlobInputNoMediaTypeInput): BlobInputNoMediaType = BlobInputNoMediaType(input)
  }
  final case class NoBodyOperation(input: NoBodyOperationInput) extends ContentHeaderTestServiceOperation[NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing] = impl.noBodyOperation(input.query)
    def ordinal: Int = 4
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing] = NoBodyOperation
  }
  object NoBodyOperation extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing] {
    val schema: OperationSchema[NoBodyOperationInput, Nothing, NoBodyOperationOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "NoBodyOperation"))
      .withInput(NoBodyOperationInput.schema)
      .withOutput(NoBodyOperationOutput.schema)
      .withHints(smithy.api.Documentation("Operation with only metadata (no body) - should not have Content-Type header"), smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/no-body"), code = 200))
    def wrap(input: NoBodyOperationInput): NoBodyOperation = NoBodyOperation(input)
  }
  final case class XmlInputJsonOutput(input: XmlInputJsonOutputInput) extends ContentHeaderTestServiceOperation[XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing] = impl.xmlInputJsonOutput(input.data)
    def ordinal: Int = 5
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing] = XmlInputJsonOutput
  }
  object XmlInputJsonOutput extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing] {
    val schema: OperationSchema[XmlInputJsonOutputInput, Nothing, XmlInputJsonOutputOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "XmlInputJsonOutput"))
      .withInput(XmlInputJsonOutputInput.schema)
      .withOutput(XmlInputJsonOutputOutput.schema)
      .withHints(smithy.api.Documentation("Operation with different media types for input and output"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/xml-json"), code = 200))
    def wrap(input: XmlInputJsonOutputInput): XmlInputJsonOutput = XmlInputJsonOutput(input)
  }
  final case class XmlInput(input: XmlInputInput) extends ContentHeaderTestServiceOperation[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing] = impl.xmlInput(input.data)
    def ordinal: Int = 6
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing] = XmlInput
  }
  object XmlInput extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing] {
    val schema: OperationSchema[XmlInputInput, Nothing, XmlInputOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "XmlInput"))
      .withInput(XmlInputInput.schema)
      .withOutput(XmlInputOutput.schema)
      .withHints(smithy.api.Documentation("Operation with XML input media type"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/xml-input"), code = 200))
    def wrap(input: XmlInputInput): XmlInput = XmlInput(input)
  }
  final case class ExplicitContentTypeHeader(input: ExplicitContentTypeHeaderInput) extends ContentHeaderTestServiceOperation[ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ContentHeaderTestServiceGen[F]): F[ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing] = impl.explicitContentTypeHeader(input.data, input.contentType)
    def ordinal: Int = 7
    def endpoint: smithy4s.Endpoint[ContentHeaderTestServiceOperation,ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing] = ExplicitContentTypeHeader
  }
  object ExplicitContentTypeHeader extends smithy4s.Endpoint[ContentHeaderTestServiceOperation,ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing] {
    val schema: OperationSchema[ExplicitContentTypeHeaderInput, Nothing, ExplicitContentTypeHeaderOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.content", "ExplicitContentTypeHeader"))
      .withInput(ExplicitContentTypeHeaderInput.schema)
      .withOutput(ExplicitContentTypeHeaderOutput.schema)
      .withHints(smithy.api.Documentation("Operation with explicit Content-Type header - should override default behavior"), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/explicit-content-type"), code = 200))
    def wrap(input: ExplicitContentTypeHeaderInput): ExplicitContentTypeHeader = ExplicitContentTypeHeader(input)
  }
}

