package smithy4s.example.imp

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.example.error.NotFoundError
import smithy4s.example.import_test.OpOutput
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.ErrorSchema
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

trait ImportServiceGen[F[_, _, _, _, _]] {
  self =>

  def importOperation(): F[Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ImportServiceGen[F]] = Transformation.of[ImportServiceGen[F]](this)
}

object ImportServiceGen extends Service.Mixin[ImportServiceGen, ImportServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.imp", "ImportService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[ImportServiceOperation, _, _, _, _, _]] = Vector(
    ImportServiceOperation.ImportOperation,
  )

  def input[I, E, O, SI, SO](op: ImportServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: ImportServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: ImportServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ImportServiceOperation.Transformed[ImportServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ImportServiceGen[ImportServiceOperation] = ImportServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ImportServiceGen[P], f: PolyFunction5[P, P1]): ImportServiceGen[P1] = new ImportServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ImportServiceOperation, P]): ImportServiceGen[P] = new ImportServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ImportServiceGen[P]): PolyFunction5[ImportServiceOperation, P] = ImportServiceOperation.toPolyFunction(impl)

  type ImportOperationError = ImportServiceOperation.ImportOperationError
  val ImportOperationError = ImportServiceOperation.ImportOperationError
}

sealed trait ImportServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ImportServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[ImportServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object ImportServiceOperation {

  object reified extends ImportServiceGen[ImportServiceOperation] {
    def importOperation(): ImportOperation = ImportOperation()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ImportServiceGen[P], f: PolyFunction5[P, P1]) extends ImportServiceGen[P1] {
    def importOperation(): P1[Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing] = f[Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing](alg.importOperation())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ImportServiceGen[P]): PolyFunction5[ImportServiceOperation, P] = new PolyFunction5[ImportServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ImportServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class ImportOperation() extends ImportServiceOperation[Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ImportServiceGen[F]): F[Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing] = impl.importOperation()
    def ordinal: Int = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[ImportServiceOperation,Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing] = ImportOperation
  }
  object ImportOperation extends smithy4s.Endpoint[ImportServiceOperation,Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, ImportServiceOperation.ImportOperationError, OpOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.import_test", "ImportOperation"))
      .withInput(unit)
      .withError(ImportOperationError.errorSchema)
      .withOutput(OpOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/test"), code = 200))
    def wrap(input: Unit): ImportOperation = ImportOperation()
  }
  sealed trait ImportOperationError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: ImportOperationError = this
    def $ordinal: Int

    object project {
      def notFoundError: Option[NotFoundError] = ImportOperationError.NotFoundErrorCase.alt.project.lift(self).map(_.notFoundError)
    }

    def accept[A](visitor: ImportOperationError.Visitor[A]): A = this match {
      case value: ImportOperationError.NotFoundErrorCase => visitor.notFoundError(value.notFoundError)
    }
  }
  object ImportOperationError extends ErrorSchema.Companion[ImportOperationError] {

    def notFoundError(notFoundError: NotFoundError): ImportOperationError = NotFoundErrorCase(notFoundError)

    val id: ShapeId = ShapeId("smithy4s.example.imp", "ImportOperationError")

    val hints: Hints = Hints.empty

    final case class NotFoundErrorCase(notFoundError: NotFoundError) extends ImportOperationError { final def $ordinal: Int = 0 }

    object NotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[ImportOperationError.NotFoundErrorCase] = bijection(NotFoundError.schema.addHints(hints), ImportOperationError.NotFoundErrorCase(_), _.notFoundError)
      val alt = schema.oneOf[ImportOperationError]("NotFoundError")
    }

    trait Visitor[A] {
      def notFoundError(value: NotFoundError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def notFoundError(value: NotFoundError): A = default
      }
    }

    implicit val schema: Schema[ImportOperationError] = union(
      ImportOperationError.NotFoundErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[ImportOperationError] = throwable match {
      case e: NotFoundError => Some(ImportOperationError.NotFoundErrorCase(e))
      case _ => None
    }
    def unliftError(e: ImportOperationError): Throwable = e match {
      case ImportOperationError.NotFoundErrorCase(e) => e
    }
  }
}

