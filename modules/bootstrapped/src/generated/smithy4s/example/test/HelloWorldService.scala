package smithy4s.example.test

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait HelloWorldServiceGen[F[_, _, _, _, _]] {
  self =>

  def hello(name: String): F[HelloInput, Nothing, HelloOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[HelloWorldServiceGen[F]] = Transformation.of[HelloWorldServiceGen[F]](this)
}

object HelloWorldServiceGen extends Service.Mixin[HelloWorldServiceGen, HelloWorldServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloWorldService")
  val version: String = "1.0.0"

  val hints: Hints = Hints.lazily(
    Hints(
      alloy.SimpleRestJson(),
    )
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[HelloWorldServiceOperation, _, _, _, _, _]] = Vector(
    HelloWorldServiceOperation.Hello,
  )

  def input[I, E, O, SI, SO](op: HelloWorldServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: HelloWorldServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: HelloWorldServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends HelloWorldServiceOperation.Transformed[HelloWorldServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: HelloWorldServiceGen[HelloWorldServiceOperation] = HelloWorldServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: HelloWorldServiceGen[P], f: PolyFunction5[P, P1]): HelloWorldServiceGen[P1] = new HelloWorldServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[HelloWorldServiceOperation, P]): HelloWorldServiceGen[P] = new HelloWorldServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: HelloWorldServiceGen[P]): PolyFunction5[HelloWorldServiceOperation, P] = HelloWorldServiceOperation.toPolyFunction(impl)

}

sealed trait HelloWorldServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: HelloWorldServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[HelloWorldServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object HelloWorldServiceOperation {

  object reified extends HelloWorldServiceGen[HelloWorldServiceOperation] {
    def hello(name: String): Hello = Hello(HelloInput(name))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: HelloWorldServiceGen[P], f: PolyFunction5[P, P1]) extends HelloWorldServiceGen[P1] {
    def hello(name: String): P1[HelloInput, Nothing, HelloOutput, Nothing, Nothing] = f[HelloInput, Nothing, HelloOutput, Nothing, Nothing](alg.hello(name))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: HelloWorldServiceGen[P]): PolyFunction5[HelloWorldServiceOperation, P] = new PolyFunction5[HelloWorldServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: HelloWorldServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Hello(input: HelloInput) extends HelloWorldServiceOperation[HelloInput, Nothing, HelloOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloWorldServiceGen[F]): F[HelloInput, Nothing, HelloOutput, Nothing, Nothing] = impl.hello(input.name)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[HelloWorldServiceOperation,HelloInput, Nothing, HelloOutput, Nothing, Nothing] = Hello
  }
  object Hello extends smithy4s.Endpoint[HelloWorldServiceOperation,HelloInput, Nothing, HelloOutput, Nothing, Nothing] {
    val schema: OperationSchema[HelloInput, Nothing, HelloOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.test", "Hello"))
      .withInput(HelloInput.schema)
      .withOutput(HelloOutput.schema)
      .withHints(smithy.test.HttpRequestTests(List(smithy.test.HttpRequestTestCase(id = "helloSuccess", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), method = "POST", uri = "/World", host = None, resolvedHost = None, authScheme = None, queryParams = None, forbidQueryParams = None, requireQueryParams = None, headers = None, forbidHeaders = None, requireHeaders = None, body = None, bodyMediaType = None, params = Some(smithy4s.Document.obj("name" -> smithy4s.Document.fromString("World"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None), smithy.test.HttpRequestTestCase(id = "helloFails", protocol = smithy4s.ShapeId(namespace = "alloy", name = "simpleRestJson"), method = "POST", uri = "/fail", host = None, resolvedHost = None, authScheme = None, queryParams = None, forbidQueryParams = None, requireQueryParams = None, headers = None, forbidHeaders = None, requireHeaders = None, body = None, bodyMediaType = None, params = Some(smithy4s.Document.obj("name" -> smithy4s.Document.fromString("World"))), vendorParams = None, vendorParamsShape = None, documentation = None, tags = None, appliesTo = None))), smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/{name}"), code = 200))
    def wrap(input: HelloInput): Hello = Hello(input)
  }
}

