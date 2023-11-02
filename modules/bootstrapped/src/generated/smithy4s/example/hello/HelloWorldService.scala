package smithy4s.example.hello

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.ErrorSchema
import smithy4s.schema.OperationSchema
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
    def hello(name: String, town: Option[String] = None): Hello = Hello(Person(name, town))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: HelloWorldServiceGen[P], f: PolyFunction5[P, P1]) extends HelloWorldServiceGen[P1] {
    def hello(name: String, town: Option[String] = None): P1[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] = f[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing](alg.hello(name, town))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: HelloWorldServiceGen[P]): PolyFunction5[HelloWorldServiceOperation, P] = new PolyFunction5[HelloWorldServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: HelloWorldServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Hello(input: Person) extends HelloWorldServiceOperation[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloWorldServiceGen[F]): F[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] = impl.hello(input.name, input.town)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[HelloWorldServiceOperation,Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] = Hello
  }
  object Hello extends smithy4s.Endpoint[HelloWorldServiceOperation,Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] {
    val schema: OperationSchema[Person, HelloWorldServiceOperation.HelloError, Greeting, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.hello", "Hello"))
      .withInput(Person.schema)
      .withError(HelloError.errorSchema)
      .withOutput(Greeting.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/{name}"), code = 200), smithy.api.Tags(List("testOperationTag")))
    def wrap(input: Person): Hello = Hello(input)
  }
  sealed trait HelloError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: HelloError = this
    def $ordinal: Int

    object project {
      def genericServerError: Option[GenericServerError] = HelloError.GenericServerErrorCase.alt.project.lift(self).map(_.genericServerError)
      def specificServerError: Option[SpecificServerError] = HelloError.SpecificServerErrorCase.alt.project.lift(self).map(_.specificServerError)
    }

    def accept[A](visitor: HelloError.Visitor[A]): A = this match {
      case value: HelloError.GenericServerErrorCase => visitor.genericServerError(value.genericServerError)
      case value: HelloError.SpecificServerErrorCase => visitor.specificServerError(value.specificServerError)
    }
  }
  object HelloError extends ErrorSchema.Companion[HelloError] {

    def genericServerError(genericServerError: GenericServerError): HelloError = GenericServerErrorCase(genericServerError)
    def specificServerError(specificServerError: SpecificServerError): HelloError = SpecificServerErrorCase(specificServerError)

    val id: ShapeId = ShapeId("smithy4s.example.hello", "HelloError")

    val hints: Hints = Hints.empty

    final case class GenericServerErrorCase(genericServerError: GenericServerError) extends HelloError { final def $ordinal: Int = 0 }
    final case class SpecificServerErrorCase(specificServerError: SpecificServerError) extends HelloError { final def $ordinal: Int = 1 }

    object GenericServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[HelloError.GenericServerErrorCase] = bijection(GenericServerError.schema.addHints(hints), HelloError.GenericServerErrorCase(_), _.genericServerError)
      val alt = schema.oneOf[HelloError]("GenericServerError")
    }
    object SpecificServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[HelloError.SpecificServerErrorCase] = bijection(SpecificServerError.schema.addHints(hints), HelloError.SpecificServerErrorCase(_), _.specificServerError)
      val alt = schema.oneOf[HelloError]("SpecificServerError")
    }

    trait Visitor[A] {
      def genericServerError(value: GenericServerError): A
      def specificServerError(value: SpecificServerError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def genericServerError(value: GenericServerError): A = default
        def specificServerError(value: SpecificServerError): A = default
      }
    }

    implicit val schema: Schema[HelloError] = union(
      HelloError.GenericServerErrorCase.alt,
      HelloError.SpecificServerErrorCase.alt,
    ){
      _.$ordinal
    }
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
}

