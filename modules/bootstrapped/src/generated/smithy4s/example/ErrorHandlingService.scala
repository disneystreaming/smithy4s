package smithy4s.example

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

trait ErrorHandlingServiceGen[F[_, _, _, _, _]] {
  self =>

  def errorHandlingOperation(in: Option[String] = None): F[ErrorHandlingOperationInput, ErrorHandlingServiceOperation.ErrorHandlingOperationError, ErrorHandlingOperationOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ErrorHandlingServiceGen[F]] = Transformation.of[ErrorHandlingServiceGen[F]](this)
}

object ErrorHandlingServiceGen extends Service.Mixin[ErrorHandlingServiceGen, ErrorHandlingServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingService")
  val version: String = "1"

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ErrorHandlingServiceOperation, _, _, _, _, _]] = Vector(
    ErrorHandlingServiceOperation.ErrorHandlingOperation,
  )

  def input[I, E, O, SI, SO](op: ErrorHandlingServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ErrorHandlingServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ErrorHandlingServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ErrorHandlingServiceOperation.Transformed[ErrorHandlingServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ErrorHandlingServiceGen[ErrorHandlingServiceOperation] = ErrorHandlingServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ErrorHandlingServiceGen[P], f: PolyFunction5[P, P1]): ErrorHandlingServiceGen[P1] = new ErrorHandlingServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ErrorHandlingServiceOperation, P]): ErrorHandlingServiceGen[P] = new ErrorHandlingServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ErrorHandlingServiceGen[P]): PolyFunction5[ErrorHandlingServiceOperation, P] = ErrorHandlingServiceOperation.toPolyFunction(impl)

  type ErrorHandlingOperationError = ErrorHandlingServiceOperation.ErrorHandlingOperationError
  val ErrorHandlingOperationError = ErrorHandlingServiceOperation.ErrorHandlingOperationError
}

sealed trait ErrorHandlingServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ErrorHandlingServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ErrorHandlingServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ErrorHandlingServiceOperation {

  object reified extends ErrorHandlingServiceGen[ErrorHandlingServiceOperation] {
    def errorHandlingOperation(in: Option[String] = None) = ErrorHandlingOperation(ErrorHandlingOperationInput(in))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ErrorHandlingServiceGen[P], f: PolyFunction5[P, P1]) extends ErrorHandlingServiceGen[P1] {
    def errorHandlingOperation(in: Option[String] = None) = f[ErrorHandlingOperationInput, ErrorHandlingServiceOperation.ErrorHandlingOperationError, ErrorHandlingOperationOutput, Nothing, Nothing](alg.errorHandlingOperation(in))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ErrorHandlingServiceGen[P]): PolyFunction5[ErrorHandlingServiceOperation, P] = new PolyFunction5[ErrorHandlingServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ErrorHandlingServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class ErrorHandlingOperation(input: ErrorHandlingOperationInput) extends ErrorHandlingServiceOperation[ErrorHandlingOperationInput, ErrorHandlingServiceOperation.ErrorHandlingOperationError, ErrorHandlingOperationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ErrorHandlingServiceGen[F]): F[ErrorHandlingOperationInput, ErrorHandlingServiceOperation.ErrorHandlingOperationError, ErrorHandlingOperationOutput, Nothing, Nothing] = impl.errorHandlingOperation(input.in)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[ErrorHandlingServiceOperation,ErrorHandlingOperationInput, ErrorHandlingServiceOperation.ErrorHandlingOperationError, ErrorHandlingOperationOutput, Nothing, Nothing] = ErrorHandlingOperation
  }
  object ErrorHandlingOperation extends smithy4s.Endpoint[ErrorHandlingServiceOperation,ErrorHandlingOperationInput, ErrorHandlingServiceOperation.ErrorHandlingOperationError, ErrorHandlingOperationOutput, Nothing, Nothing] with Errorable[ErrorHandlingOperationError] {
    val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperation")
    val input: Schema[ErrorHandlingOperationInput] = ErrorHandlingOperationInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[ErrorHandlingOperationOutput] = ErrorHandlingOperationOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: ErrorHandlingOperationInput) = ErrorHandlingOperation(input)
    override val errorable: Option[Errorable[ErrorHandlingOperationError]] = Some(this)
    val error: UnionSchema[ErrorHandlingOperationError] = ErrorHandlingOperationError.schema
    def liftError(throwable: Throwable): Option[ErrorHandlingOperationError] = throwable match {
      case e: EHFallbackClientError => Some(ErrorHandlingOperationError.EHFallbackClientErrorCase(e))
      case e: EHServiceUnavailable => Some(ErrorHandlingOperationError.EHServiceUnavailableCase(e))
      case e: EHNotFound => Some(ErrorHandlingOperationError.EHNotFoundCase(e))
      case e: EHFallbackServerError => Some(ErrorHandlingOperationError.EHFallbackServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: ErrorHandlingOperationError): Throwable = e match {
      case ErrorHandlingOperationError.EHFallbackClientErrorCase(e) => e
      case ErrorHandlingOperationError.EHServiceUnavailableCase(e) => e
      case ErrorHandlingOperationError.EHNotFoundCase(e) => e
      case ErrorHandlingOperationError.EHFallbackServerErrorCase(e) => e
    }
  }
  sealed trait ErrorHandlingOperationError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: ErrorHandlingOperationError = this
    def $ordinal: Int

    object project {
      def eHFallbackClientError: Option[EHFallbackClientError] = ErrorHandlingOperationError.EHFallbackClientErrorCase.alt.project.lift(self).map(_.eHFallbackClientError)
      def eHServiceUnavailable: Option[EHServiceUnavailable] = ErrorHandlingOperationError.EHServiceUnavailableCase.alt.project.lift(self).map(_.eHServiceUnavailable)
      def eHNotFound: Option[EHNotFound] = ErrorHandlingOperationError.EHNotFoundCase.alt.project.lift(self).map(_.eHNotFound)
      def eHFallbackServerError: Option[EHFallbackServerError] = ErrorHandlingOperationError.EHFallbackServerErrorCase.alt.project.lift(self).map(_.eHFallbackServerError)
    }

    def accept[A](visitor: ErrorHandlingOperationError.Visitor[A]): A = this match {
      case value: ErrorHandlingOperationError.EHFallbackClientErrorCase => visitor.eHFallbackClientError(value.eHFallbackClientError)
      case value: ErrorHandlingOperationError.EHServiceUnavailableCase => visitor.eHServiceUnavailable(value.eHServiceUnavailable)
      case value: ErrorHandlingOperationError.EHNotFoundCase => visitor.eHNotFound(value.eHNotFound)
      case value: ErrorHandlingOperationError.EHFallbackServerErrorCase => visitor.eHFallbackServerError(value.eHFallbackServerError)
    }
  }
  object ErrorHandlingOperationError extends ShapeTag.Companion[ErrorHandlingOperationError] {

    def eHFallbackClientError(eHFallbackClientError: EHFallbackClientError): ErrorHandlingOperationError = EHFallbackClientErrorCase(eHFallbackClientError)
    def eHServiceUnavailable(eHServiceUnavailable: EHServiceUnavailable): ErrorHandlingOperationError = EHServiceUnavailableCase(eHServiceUnavailable)
    def eHNotFound(eHNotFound: EHNotFound): ErrorHandlingOperationError = EHNotFoundCase(eHNotFound)
    def eHFallbackServerError(eHFallbackServerError: EHFallbackServerError): ErrorHandlingOperationError = EHFallbackServerErrorCase(eHFallbackServerError)

    val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationError")

    val hints: Hints = Hints.empty

    final case class EHFallbackClientErrorCase(eHFallbackClientError: EHFallbackClientError) extends ErrorHandlingOperationError { final def $ordinal: Int = 0 }
    final case class EHServiceUnavailableCase(eHServiceUnavailable: EHServiceUnavailable) extends ErrorHandlingOperationError { final def $ordinal: Int = 1 }
    final case class EHNotFoundCase(eHNotFound: EHNotFound) extends ErrorHandlingOperationError { final def $ordinal: Int = 2 }
    final case class EHFallbackServerErrorCase(eHFallbackServerError: EHFallbackServerError) extends ErrorHandlingOperationError { final def $ordinal: Int = 3 }

    object EHFallbackClientErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ErrorHandlingOperationError.EHFallbackClientErrorCase] = bijection(EHFallbackClientError.schema.addHints(hints), ErrorHandlingOperationError.EHFallbackClientErrorCase(_), _.eHFallbackClientError)
      val alt = schema.oneOf[ErrorHandlingOperationError]("EHFallbackClientError")
    }
    object EHServiceUnavailableCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ErrorHandlingOperationError.EHServiceUnavailableCase] = bijection(EHServiceUnavailable.schema.addHints(hints), ErrorHandlingOperationError.EHServiceUnavailableCase(_), _.eHServiceUnavailable)
      val alt = schema.oneOf[ErrorHandlingOperationError]("EHServiceUnavailable")
    }
    object EHNotFoundCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ErrorHandlingOperationError.EHNotFoundCase] = bijection(EHNotFound.schema.addHints(hints), ErrorHandlingOperationError.EHNotFoundCase(_), _.eHNotFound)
      val alt = schema.oneOf[ErrorHandlingOperationError]("EHNotFound")
    }
    object EHFallbackServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ErrorHandlingOperationError.EHFallbackServerErrorCase] = bijection(EHFallbackServerError.schema.addHints(hints), ErrorHandlingOperationError.EHFallbackServerErrorCase(_), _.eHFallbackServerError)
      val alt = schema.oneOf[ErrorHandlingOperationError]("EHFallbackServerError")
    }

    trait Visitor[A] {
      def eHFallbackClientError(value: EHFallbackClientError): A
      def eHServiceUnavailable(value: EHServiceUnavailable): A
      def eHNotFound(value: EHNotFound): A
      def eHFallbackServerError(value: EHFallbackServerError): A
    }

    implicit val schema: UnionSchema[ErrorHandlingOperationError] = union(
      ErrorHandlingOperationError.EHFallbackClientErrorCase.alt,
      ErrorHandlingOperationError.EHServiceUnavailableCase.alt,
      ErrorHandlingOperationError.EHNotFoundCase.alt,
      ErrorHandlingOperationError.EHFallbackServerErrorCase.alt,
    ){
      _.$ordinal
    }
  }
}

