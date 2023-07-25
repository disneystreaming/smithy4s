package smithy4s.example.hello

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

trait HelloWorldServiceGen[F[_, _, _, _, _]] {
  self =>

  def hello(name: String, town: Option[String] = None): F[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[HelloWorldServiceGen[F]] = Transformation.of[HelloWorldServiceGen[F]](this)
}

object HelloWorldServiceGen extends Service.Mixin[HelloWorldServiceGen, HelloWorldServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.hello", "HelloWorldService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
    smithy.api.Tags(List("testServiceTag")),
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

  type HelloError = HelloWorldServiceOperation.HelloError
  val HelloError = HelloWorldServiceOperation.HelloError
}

sealed trait HelloWorldServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: HelloWorldServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[HelloWorldServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object HelloWorldServiceOperation {

  object reified extends HelloWorldServiceGen[HelloWorldServiceOperation] {
    def hello(name: String, town: Option[String] = None) = Hello(Person(name, town))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: HelloWorldServiceGen[P], f: PolyFunction5[P, P1]) extends HelloWorldServiceGen[P1] {
    def hello(name: String, town: Option[String] = None) = f[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing](alg.hello(name, town))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: HelloWorldServiceGen[P]): PolyFunction5[HelloWorldServiceOperation, P] = new PolyFunction5[HelloWorldServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: HelloWorldServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Hello(input: Person) extends HelloWorldServiceOperation[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloWorldServiceGen[F]): F[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] = impl.hello(input.name, input.town)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[HelloWorldServiceOperation,Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] = Hello
  }
  object Hello extends smithy4s.Endpoint[HelloWorldServiceOperation,Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] with Errorable[HelloError] {
    val id: ShapeId = ShapeId("smithy4s.example.hello", "Hello")
    val input: Schema[Person] = Person.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Greeting] = Greeting.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/{name}"), code = 200),
      smithy.api.Tags(List("testOperationTag")),
    )
    def wrap(input: Person) = Hello(input)
    override val errorable: Option[Errorable[HelloError]] = Some(this)
    val error: UnionSchema[HelloError] = HelloError.schema
    def liftError(throwable: Throwable): Option[HelloError] = throwable match {
      case e: GenericServerError => Some(HelloError.GenericServerErrorCase(e))
      case e: SpecificServerError => Some(HelloError.SpecificServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: HelloError): Throwable = e match {
      case HelloError.GenericServerErrorCase(e) => e
      case HelloError.SpecificServerErrorCase(e) => e
    }
  }
  sealed trait HelloError extends scala.Product with scala.Serializable {
    @inline final def widen: HelloError = this
    def _ordinal: Int
  }
  object HelloError extends ShapeTag.Companion[HelloError] {
    val hints: Hints = Hints.empty

    final case class GenericServerErrorCase(genericServerError: GenericServerError) extends HelloError { final def _ordinal: Int = 0 }
    def genericServerError(genericServerError:GenericServerError): HelloError = GenericServerErrorCase(genericServerError)
    final case class SpecificServerErrorCase(specificServerError: SpecificServerError) extends HelloError { final def _ordinal: Int = 1 }
    def specificServerError(specificServerError:SpecificServerError): HelloError = SpecificServerErrorCase(specificServerError)

    object GenericServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GenericServerErrorCase] = bijection(GenericServerError.schema.addHints(hints), GenericServerErrorCase(_), _.genericServerError)
      val alt = schema.oneOf[HelloError]("GenericServerError")
    }
    object SpecificServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[SpecificServerErrorCase] = bijection(SpecificServerError.schema.addHints(hints), SpecificServerErrorCase(_), _.specificServerError)
      val alt = schema.oneOf[HelloError]("SpecificServerError")
    }

    implicit val schema: UnionSchema[HelloError] = union(
      GenericServerErrorCase.alt,
      SpecificServerErrorCase.alt,
    ){
      _._ordinal
    }
  }
}

