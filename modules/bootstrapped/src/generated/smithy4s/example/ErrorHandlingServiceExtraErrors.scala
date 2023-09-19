package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Errorable
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.UnionSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

trait ErrorHandlingServiceExtraErrorsGen[F[_, _, _, _, _]] {
  self =>

  def extraErrorOperation(in: Option[String] = None): F[ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ErrorHandlingServiceExtraErrorsGen[F]] = Transformation.of[ErrorHandlingServiceExtraErrorsGen[F]](this)
}

object ErrorHandlingServiceExtraErrorsGen extends Service.Mixin[ErrorHandlingServiceExtraErrorsGen, ErrorHandlingServiceExtraErrorsOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingServiceExtraErrors")
  val version: String = "1"

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ErrorHandlingServiceExtraErrorsOperation, _, _, _, _, _]] = Vector(
    ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperation,
  )

  def input[I, E, O, SI, SO](op: ErrorHandlingServiceExtraErrorsOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ErrorHandlingServiceExtraErrorsOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ErrorHandlingServiceExtraErrorsOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ErrorHandlingServiceExtraErrorsOperation.Transformed[ErrorHandlingServiceExtraErrorsOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ErrorHandlingServiceExtraErrorsGen[ErrorHandlingServiceExtraErrorsOperation] = ErrorHandlingServiceExtraErrorsOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ErrorHandlingServiceExtraErrorsGen[P], f: PolyFunction5[P, P1]): ErrorHandlingServiceExtraErrorsGen[P1] = new ErrorHandlingServiceExtraErrorsOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ErrorHandlingServiceExtraErrorsOperation, P]): ErrorHandlingServiceExtraErrorsGen[P] = new ErrorHandlingServiceExtraErrorsOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ErrorHandlingServiceExtraErrorsGen[P]): PolyFunction5[ErrorHandlingServiceExtraErrorsOperation, P] = ErrorHandlingServiceExtraErrorsOperation.toPolyFunction(impl)

  type ExtraErrorOperationError = ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError
  val ExtraErrorOperationError = ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError
}

sealed trait ErrorHandlingServiceExtraErrorsOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ErrorHandlingServiceExtraErrorsGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ErrorHandlingServiceExtraErrorsOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ErrorHandlingServiceExtraErrorsOperation {

  object reified extends ErrorHandlingServiceExtraErrorsGen[ErrorHandlingServiceExtraErrorsOperation] {
    def extraErrorOperation(in: Option[String] = None) = ExtraErrorOperation(ExtraErrorOperationInput(in))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ErrorHandlingServiceExtraErrorsGen[P], f: PolyFunction5[P, P1]) extends ErrorHandlingServiceExtraErrorsGen[P1] {
    def extraErrorOperation(in: Option[String] = None) = f[ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing](alg.extraErrorOperation(in))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ErrorHandlingServiceExtraErrorsGen[P]): PolyFunction5[ErrorHandlingServiceExtraErrorsOperation, P] = new PolyFunction5[ErrorHandlingServiceExtraErrorsOperation, P] {
    def apply[I, E, O, SI, SO](op: ErrorHandlingServiceExtraErrorsOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class ExtraErrorOperation(input: ExtraErrorOperationInput) extends ErrorHandlingServiceExtraErrorsOperation[ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ErrorHandlingServiceExtraErrorsGen[F]): F[ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing] = impl.extraErrorOperation(input.in)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[ErrorHandlingServiceExtraErrorsOperation,ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing] = ExtraErrorOperation
  }
  object ExtraErrorOperation extends smithy4s.Endpoint[ErrorHandlingServiceExtraErrorsOperation,ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing] {
    def schema: OperationSchema[ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "ExtraErrorOperation"))
      .withInput(ExtraErrorOperationInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withError(ExtraErrorOperationError)
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: ExtraErrorOperationInput) = ExtraErrorOperation(input)
  }
  sealed trait ExtraErrorOperationError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: ExtraErrorOperationError = this
    def $ordinal: Int

    object project {
      def randomOtherClientError: Option[RandomOtherClientError] = ExtraErrorOperationError.RandomOtherClientErrorCase.alt.project.lift(self).map(_.randomOtherClientError)
      def randomOtherServerError: Option[RandomOtherServerError] = ExtraErrorOperationError.RandomOtherServerErrorCase.alt.project.lift(self).map(_.randomOtherServerError)
      def randomOtherClientErrorWithCode: Option[RandomOtherClientErrorWithCode] = ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase.alt.project.lift(self).map(_.randomOtherClientErrorWithCode)
      def randomOtherServerErrorWithCode: Option[RandomOtherServerErrorWithCode] = ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase.alt.project.lift(self).map(_.randomOtherServerErrorWithCode)
    }

    def accept[A](visitor: ExtraErrorOperationError.Visitor[A]): A = this match {
      case value: ExtraErrorOperationError.RandomOtherClientErrorCase => visitor.randomOtherClientError(value.randomOtherClientError)
      case value: ExtraErrorOperationError.RandomOtherServerErrorCase => visitor.randomOtherServerError(value.randomOtherServerError)
      case value: ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase => visitor.randomOtherClientErrorWithCode(value.randomOtherClientErrorWithCode)
      case value: ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase => visitor.randomOtherServerErrorWithCode(value.randomOtherServerErrorWithCode)
    }
  }
  object ExtraErrorOperationError extends Errorable.Companion[ExtraErrorOperationError] {

    def randomOtherClientError(randomOtherClientError: RandomOtherClientError): ExtraErrorOperationError = RandomOtherClientErrorCase(randomOtherClientError)
    def randomOtherServerError(randomOtherServerError: RandomOtherServerError): ExtraErrorOperationError = RandomOtherServerErrorCase(randomOtherServerError)
    def randomOtherClientErrorWithCode(randomOtherClientErrorWithCode: RandomOtherClientErrorWithCode): ExtraErrorOperationError = RandomOtherClientErrorWithCodeCase(randomOtherClientErrorWithCode)
    def randomOtherServerErrorWithCode(randomOtherServerErrorWithCode: RandomOtherServerErrorWithCode): ExtraErrorOperationError = RandomOtherServerErrorWithCodeCase(randomOtherServerErrorWithCode)

    val id: ShapeId = ShapeId("smithy4s.example", "ExtraErrorOperationError")

    val hints: Hints = Hints.empty

    final case class RandomOtherClientErrorCase(randomOtherClientError: RandomOtherClientError) extends ExtraErrorOperationError { final def $ordinal: Int = 0 }
    final case class RandomOtherServerErrorCase(randomOtherServerError: RandomOtherServerError) extends ExtraErrorOperationError { final def $ordinal: Int = 1 }
    final case class RandomOtherClientErrorWithCodeCase(randomOtherClientErrorWithCode: RandomOtherClientErrorWithCode) extends ExtraErrorOperationError { final def $ordinal: Int = 2 }
    final case class RandomOtherServerErrorWithCodeCase(randomOtherServerErrorWithCode: RandomOtherServerErrorWithCode) extends ExtraErrorOperationError { final def $ordinal: Int = 3 }

    object RandomOtherClientErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ExtraErrorOperationError.RandomOtherClientErrorCase] = bijection(RandomOtherClientError.schema.addHints(hints), ExtraErrorOperationError.RandomOtherClientErrorCase(_), _.randomOtherClientError)
      val alt = schema.oneOf[ExtraErrorOperationError]("RandomOtherClientError")
    }
    object RandomOtherServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ExtraErrorOperationError.RandomOtherServerErrorCase] = bijection(RandomOtherServerError.schema.addHints(hints), ExtraErrorOperationError.RandomOtherServerErrorCase(_), _.randomOtherServerError)
      val alt = schema.oneOf[ExtraErrorOperationError]("RandomOtherServerError")
    }
    object RandomOtherClientErrorWithCodeCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase] = bijection(RandomOtherClientErrorWithCode.schema.addHints(hints), ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase(_), _.randomOtherClientErrorWithCode)
      val alt = schema.oneOf[ExtraErrorOperationError]("RandomOtherClientErrorWithCode")
    }
    object RandomOtherServerErrorWithCodeCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase] = bijection(RandomOtherServerErrorWithCode.schema.addHints(hints), ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase(_), _.randomOtherServerErrorWithCode)
      val alt = schema.oneOf[ExtraErrorOperationError]("RandomOtherServerErrorWithCode")
    }

    trait Visitor[A] {
      def randomOtherClientError(value: RandomOtherClientError): A
      def randomOtherServerError(value: RandomOtherServerError): A
      def randomOtherClientErrorWithCode(value: RandomOtherClientErrorWithCode): A
      def randomOtherServerErrorWithCode(value: RandomOtherServerErrorWithCode): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def randomOtherClientError(value: RandomOtherClientError): A = default
        def randomOtherServerError(value: RandomOtherServerError): A = default
        def randomOtherClientErrorWithCode(value: RandomOtherClientErrorWithCode): A = default
        def randomOtherServerErrorWithCode(value: RandomOtherServerErrorWithCode): A = default
      }
    }

    implicit val schema: UnionSchema[ExtraErrorOperationError] = union(
      ExtraErrorOperationError.RandomOtherClientErrorCase.alt,
      ExtraErrorOperationError.RandomOtherServerErrorCase.alt,
      ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase.alt,
      ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[ExtraErrorOperationError] = throwable match {
      case e: RandomOtherClientError => Some(ExtraErrorOperationError.RandomOtherClientErrorCase(e))
      case e: RandomOtherServerError => Some(ExtraErrorOperationError.RandomOtherServerErrorCase(e))
      case e: RandomOtherClientErrorWithCode => Some(ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase(e))
      case e: RandomOtherServerErrorWithCode => Some(ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase(e))
      case _ => None
    }
    def unliftError(e: ExtraErrorOperationError): Throwable = e match {
      case ExtraErrorOperationError.RandomOtherClientErrorCase(e) => e
      case ExtraErrorOperationError.RandomOtherServerErrorCase(e) => e
      case ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase(e) => e
      case ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase(e) => e
    }
  }
}

