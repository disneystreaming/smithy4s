package smithy4s.example.test

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

trait HelloServiceGen[F[_, _, _, _, _]] {
  self =>

  def sayHello(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None): F[SayHelloInput, HelloServiceOperation.SayHelloError, SayHelloOutput, Nothing, Nothing]
  def listen(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def testPath(path: String): F[TestPathInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[HelloServiceGen[F]] = Transformation.of[HelloServiceGen[F]](this)
}

object HelloServiceGen extends Service.Mixin[HelloServiceGen, HelloServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloService")
  val version: String = ""

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[HelloServiceOperation, _, _, _, _, _]] = Vector(
    HelloServiceOperation.SayHello,
    HelloServiceOperation.Listen,
    HelloServiceOperation.TestPath,
  )

  def input[I, E, O, SI, SO](op: HelloServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: HelloServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: HelloServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends HelloServiceOperation.Transformed[HelloServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: HelloServiceGen[HelloServiceOperation] = HelloServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: HelloServiceGen[P], f: PolyFunction5[P, P1]): HelloServiceGen[P1] = new HelloServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[HelloServiceOperation, P]): HelloServiceGen[P] = new HelloServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: HelloServiceGen[P]): PolyFunction5[HelloServiceOperation, P] = HelloServiceOperation.toPolyFunction(impl)

  type SayHelloError = HelloServiceOperation.SayHelloError
  val SayHelloError = HelloServiceOperation.SayHelloError
}

sealed trait HelloServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: HelloServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[HelloServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object HelloServiceOperation {

  object reified extends HelloServiceGen[HelloServiceOperation] {
    def sayHello(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None) = SayHello(SayHelloInput(greeting, query, name))
    def listen() = Listen()
    def testPath(path: String) = TestPath(TestPathInput(path))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: HelloServiceGen[P], f: PolyFunction5[P, P1]) extends HelloServiceGen[P1] {
    def sayHello(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None) = f[SayHelloInput, HelloServiceOperation.SayHelloError, SayHelloOutput, Nothing, Nothing](alg.sayHello(greeting, query, name))
    def listen() = f[Unit, Nothing, Unit, Nothing, Nothing](alg.listen())
    def testPath(path: String) = f[TestPathInput, Nothing, Unit, Nothing, Nothing](alg.testPath(path))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: HelloServiceGen[P]): PolyFunction5[HelloServiceOperation, P] = new PolyFunction5[HelloServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: HelloServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class SayHello(input: SayHelloInput) extends HelloServiceOperation[SayHelloInput, HelloServiceOperation.SayHelloError, SayHelloOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloServiceGen[F]): F[SayHelloInput, HelloServiceOperation.SayHelloError, SayHelloOutput, Nothing, Nothing] = impl.sayHello(input.greeting, input.query, input.name)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[HelloServiceOperation,SayHelloInput, HelloServiceOperation.SayHelloError, SayHelloOutput, Nothing, Nothing] = SayHello
  }
  object SayHello extends smithy4s.Endpoint[HelloServiceOperation,SayHelloInput, HelloServiceOperation.SayHelloError, SayHelloOutput, Nothing, Nothing] with Errorable[SayHelloError] {
    val id: ShapeId = ShapeId("smithy4s.example.test", "SayHello")
    val input: Schema[SayHelloInput] = SayHelloInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[SayHelloOutput] = SayHelloOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.test.HttpRequestTests(List(smithy.test.HttpRequestTestCase(id = "say_hello", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), method = "POST", uri = "/", host = None, resolvedHost = None, authScheme = None, queryParams = Some(List("Hi=Hello%20there")), forbidQueryParams = None, requireQueryParams = None, headers = Some(Map("X-Greeting" -> "Hi")), forbidHeaders = None, requireHeaders = None, body = Some("{\"name\":\"Teddy\"}"), bodyMediaType = Some("application/json"), params = Some(smithy4s.Document.obj("greeting" -> smithy4s.Document.fromString("Hi"), "name" -> smithy4s.Document.fromString("Teddy"), "query" -> smithy4s.Document.fromString("Hello there"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/"), code = 200),
      smithy.test.HttpResponseTests(List(smithy.test.HttpResponseTestCase(id = "say_hello", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), code = 200, authScheme = None, headers = Some(Map("X-H1" -> "V1")), forbidHeaders = None, requireHeaders = None, body = Some("{\"result\":\"Hello!\"}"), bodyMediaType = None, params = Some(smithy4s.Document.obj("payload" -> smithy4s.Document.obj("result" -> smithy4s.Document.fromString("Hello!")), "header1" -> smithy4s.Document.fromString("V1"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
    )
    def wrap(input: SayHelloInput) = SayHello(input)
    override val errorable: Option[Errorable[SayHelloError]] = Some(this)
    val error: UnionSchema[SayHelloError] = SayHelloError.schema
    def liftError(throwable: Throwable): Option[SayHelloError] = throwable match {
      case e: SimpleError => Some(SayHelloError.SimpleErrorCase(e))
      case e: ComplexError => Some(SayHelloError.ComplexErrorCase(e))
      case _ => None
    }
    def unliftError(e: SayHelloError): Throwable = e match {
      case SayHelloError.SimpleErrorCase(e) => e
      case SayHelloError.ComplexErrorCase(e) => e
    }
  }
  sealed trait SayHelloError extends scala.Product with scala.Serializable {
    @inline final def widen: SayHelloError = this
    def $ordinal: Int
  }
  object SayHelloError extends ShapeTag.Companion[SayHelloError] {

    def simpleError(simpleError:SimpleError): SayHelloError = SimpleErrorCase(simpleError)
    def complexError(complexError:ComplexError): SayHelloError = ComplexErrorCase(complexError)

    val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloError")

    val hints: Hints = Hints.empty

    final case class SimpleErrorCase(simpleError: SimpleError) extends SayHelloError { final def $ordinal: Int = 0 }
    final case class ComplexErrorCase(complexError: ComplexError) extends SayHelloError { final def $ordinal: Int = 1 }

    object SimpleErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[SimpleErrorCase] = bijection(SimpleError.schema.addHints(hints), SimpleErrorCase(_), _.simpleError)
      val alt = schema.oneOf[SayHelloError]("SimpleError")
    }
    object ComplexErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ComplexErrorCase] = bijection(ComplexError.schema.addHints(hints), ComplexErrorCase(_), _.complexError)
      val alt = schema.oneOf[SayHelloError]("ComplexError")
    }

    implicit val schema: UnionSchema[SayHelloError] = union(
      SimpleErrorCase.alt,
      ComplexErrorCase.alt,
    ){
      _.$ordinal
    }
  }
  final case class Listen() extends HelloServiceOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloServiceGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl.listen()
    def ordinal = 1
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[HelloServiceOperation,Unit, Nothing, Unit, Nothing, Nothing] = Listen
  }
  object Listen extends smithy4s.Endpoint[HelloServiceOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.test", "Listen")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.test.HttpRequestTests(List(smithy.test.HttpRequestTestCase(id = "listen", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), method = "GET", uri = "/listen", host = None, resolvedHost = None, authScheme = None, queryParams = None, forbidQueryParams = None, requireQueryParams = None, headers = None, forbidHeaders = None, requireHeaders = None, body = None, bodyMediaType = None, params = None, vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/listen"), code = 200),
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = Listen()
    override val errorable: Option[Nothing] = None
  }
  final case class TestPath(input: TestPathInput) extends HelloServiceOperation[TestPathInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloServiceGen[F]): F[TestPathInput, Nothing, Unit, Nothing, Nothing] = impl.testPath(input.path)
    def ordinal = 2
    def endpoint: smithy4s.Endpoint[HelloServiceOperation,TestPathInput, Nothing, Unit, Nothing, Nothing] = TestPath
  }
  object TestPath extends smithy4s.Endpoint[HelloServiceOperation,TestPathInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.test", "TestPath")
    val input: Schema[TestPathInput] = TestPathInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.test.HttpRequestTests(List(smithy.test.HttpRequestTestCase(id = "TestPath", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), method = "GET", uri = "/test-path/sameValue", host = None, resolvedHost = None, authScheme = None, queryParams = None, forbidQueryParams = None, requireQueryParams = None, headers = None, forbidHeaders = None, requireHeaders = None, body = None, bodyMediaType = None, params = Some(smithy4s.Document.obj("path" -> smithy4s.Document.fromString("sameValue"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))),
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/test-path/{path}"), code = 200),
      smithy.api.Readonly(),
    )
    def wrap(input: TestPathInput) = TestPath(input)
    override val errorable: Option[Nothing] = None
  }
}

