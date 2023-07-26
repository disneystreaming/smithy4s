package smithy4s.example

import smithy4s.Bijection
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

trait ErrorHandlingServiceExtraErrorsGen[F[_, _, _, _, _]] {
  self =>

  def extraErrorOperation(in: Option[String] = None): F[ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ErrorHandlingServiceExtraErrorsGen[F]] = Transformation.of[ErrorHandlingServiceExtraErrorsGen[F]](this)
}

object ErrorHandlingServiceExtraErrorsGen extends Service.Mixin[ErrorHandlingServiceExtraErrorsGen, ErrorHandlingServiceExtraErrorsOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingServiceExtraErrors")
  val version: String = "1"

  val hints: Hints =
  Hints.empty

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
  object ExtraErrorOperation extends smithy4s.Endpoint[ErrorHandlingServiceExtraErrorsOperation,ExtraErrorOperationInput, ErrorHandlingServiceExtraErrorsOperation.ExtraErrorOperationError, Unit, Nothing, Nothing] with Errorable[ExtraErrorOperationError] {
    val id: ShapeId = ShapeId("smithy4s.example", "ExtraErrorOperation")
    val input: Schema[ExtraErrorOperationInput] = ExtraErrorOperationInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints.empty
    def wrap(input: ExtraErrorOperationInput) = ExtraErrorOperation(input)
    override val errorable: Option[Errorable[ExtraErrorOperationError]] = Some(this)
    val error: UnionSchema[ExtraErrorOperationError] = ExtraErrorOperationError.schema
    def liftError(throwable: Throwable): Option[ExtraErrorOperationError] = throwable match {
      case e: smithy4s.example.RandomOtherClientError => Some(ExtraErrorOperationError.RandomOtherClientErrorCase(e))
      case e: smithy4s.example.RandomOtherServerError => Some(ExtraErrorOperationError.RandomOtherServerErrorCase(e))
      case e: smithy4s.example.RandomOtherClientErrorWithCode => Some(ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase(e))
      case e: smithy4s.example.RandomOtherServerErrorWithCode => Some(ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase(e))
      case _ => None
    }
    def unliftError(e: ExtraErrorOperationError): Throwable = e match {
      case ExtraErrorOperationError.RandomOtherClientErrorCase(e) => e
      case ExtraErrorOperationError.RandomOtherServerErrorCase(e) => e
      case ExtraErrorOperationError.RandomOtherClientErrorWithCodeCase(e) => e
      case ExtraErrorOperationError.RandomOtherServerErrorWithCodeCase(e) => e
    }
  }
  sealed trait ExtraErrorOperationError extends scala.Product with scala.Serializable {
    @inline final def widen: ExtraErrorOperationError = this
    def _ordinal: Int
  }
  object ExtraErrorOperationError extends ShapeTag.Companion[ExtraErrorOperationError] {
    final case class RandomOtherClientErrorCase(randomOtherClientError: smithy4s.example.RandomOtherClientError) extends ExtraErrorOperationError { final def _ordinal: Int = 0 }
    final case class RandomOtherServerErrorCase(randomOtherServerError: smithy4s.example.RandomOtherServerError) extends ExtraErrorOperationError { final def _ordinal: Int = 1 }
    final case class RandomOtherClientErrorWithCodeCase(randomOtherClientErrorWithCode: smithy4s.example.RandomOtherClientErrorWithCode) extends ExtraErrorOperationError { final def _ordinal: Int = 2 }
    final case class RandomOtherServerErrorWithCodeCase(randomOtherServerErrorWithCode: smithy4s.example.RandomOtherServerErrorWithCode) extends ExtraErrorOperationError { final def _ordinal: Int = 3 }

    object RandomOtherClientErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.RandomOtherClientError, RandomOtherClientErrorCase] = Bijection(RandomOtherClientErrorCase(_), _.randomOtherClientError)
      implicit val toValue: Bijection[RandomOtherClientErrorCase, smithy4s.example.RandomOtherClientError] = fromValue.swap
      val schema: Schema[RandomOtherClientErrorCase] = bijection(smithy4s.example.RandomOtherClientError.schema, fromValue)
    }
    object RandomOtherServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.RandomOtherServerError, RandomOtherServerErrorCase] = Bijection(RandomOtherServerErrorCase(_), _.randomOtherServerError)
      implicit val toValue: Bijection[RandomOtherServerErrorCase, smithy4s.example.RandomOtherServerError] = fromValue.swap
      val schema: Schema[RandomOtherServerErrorCase] = bijection(smithy4s.example.RandomOtherServerError.schema, fromValue)
    }
    object RandomOtherClientErrorWithCodeCase {
      implicit val fromValue: Bijection[smithy4s.example.RandomOtherClientErrorWithCode, RandomOtherClientErrorWithCodeCase] = Bijection(RandomOtherClientErrorWithCodeCase(_), _.randomOtherClientErrorWithCode)
      implicit val toValue: Bijection[RandomOtherClientErrorWithCodeCase, smithy4s.example.RandomOtherClientErrorWithCode] = fromValue.swap
      val schema: Schema[RandomOtherClientErrorWithCodeCase] = bijection(smithy4s.example.RandomOtherClientErrorWithCode.schema, fromValue)
    }
    object RandomOtherServerErrorWithCodeCase {
      implicit val fromValue: Bijection[smithy4s.example.RandomOtherServerErrorWithCode, RandomOtherServerErrorWithCodeCase] = Bijection(RandomOtherServerErrorWithCodeCase(_), _.randomOtherServerErrorWithCode)
      implicit val toValue: Bijection[RandomOtherServerErrorWithCodeCase, smithy4s.example.RandomOtherServerErrorWithCode] = fromValue.swap
      val schema: Schema[RandomOtherServerErrorWithCodeCase] = bijection(smithy4s.example.RandomOtherServerErrorWithCode.schema, fromValue)
    }

    val RandomOtherClientError = RandomOtherClientErrorCase.schema.oneOf[ExtraErrorOperationError]("RandomOtherClientError")
    val RandomOtherServerError = RandomOtherServerErrorCase.schema.oneOf[ExtraErrorOperationError]("RandomOtherServerError")
    val RandomOtherClientErrorWithCode = RandomOtherClientErrorWithCodeCase.schema.oneOf[ExtraErrorOperationError]("RandomOtherClientErrorWithCode")
    val RandomOtherServerErrorWithCode = RandomOtherServerErrorWithCodeCase.schema.oneOf[ExtraErrorOperationError]("RandomOtherServerErrorWithCode")

    implicit val schema: UnionSchema[ExtraErrorOperationError] = union(
      RandomOtherClientError,
      RandomOtherServerError,
      RandomOtherClientErrorWithCode,
      RandomOtherServerErrorWithCode,
    ){
      _._ordinal
    }
    
  }
}

