package smithy4s.example.grpc

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

trait GrpcGreetingServiceGen[F[_, _, _, _, _]] {
  self =>

  def greet(name: String): F[GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[GrpcGreetingServiceGen[F]] = Transformation.of[GrpcGreetingServiceGen[F]](this)
}

object GrpcGreetingServiceGen extends Service.Mixin[GrpcGreetingServiceGen, GrpcGreetingServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.grpc", "GrpcGreetingService")
  val version: String = ""

  val hints: Hints = Hints(
    alloy.proto.Grpc(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[GrpcGreetingServiceOperation, _, _, _, _, _]] = Vector(
    GrpcGreetingServiceOperation.Greet,
  )

  def input[I, E, O, SI, SO](op: GrpcGreetingServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: GrpcGreetingServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: GrpcGreetingServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends GrpcGreetingServiceOperation.Transformed[GrpcGreetingServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: GrpcGreetingServiceGen[GrpcGreetingServiceOperation] = GrpcGreetingServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: GrpcGreetingServiceGen[P], f: PolyFunction5[P, P1]): GrpcGreetingServiceGen[P1] = new GrpcGreetingServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[GrpcGreetingServiceOperation, P]): GrpcGreetingServiceGen[P] = new GrpcGreetingServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: GrpcGreetingServiceGen[P]): PolyFunction5[GrpcGreetingServiceOperation, P] = GrpcGreetingServiceOperation.toPolyFunction(impl)

  type GreetError = GrpcGreetingServiceOperation.GreetError
  val GreetError = GrpcGreetingServiceOperation.GreetError
}

sealed trait GrpcGreetingServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: GrpcGreetingServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[GrpcGreetingServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object GrpcGreetingServiceOperation {

  object reified extends GrpcGreetingServiceGen[GrpcGreetingServiceOperation] {
    def greet(name: String): Greet = Greet(GreetInput(name))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: GrpcGreetingServiceGen[P], f: PolyFunction5[P, P1]) extends GrpcGreetingServiceGen[P1] {
    def greet(name: String): P1[GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing] = f[GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing](this.alg.greet(name))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: GrpcGreetingServiceGen[P]): PolyFunction5[GrpcGreetingServiceOperation, P] = new PolyFunction5[GrpcGreetingServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: GrpcGreetingServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Greet(input: GreetInput) extends GrpcGreetingServiceOperation[GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: GrpcGreetingServiceGen[F]): F[GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing] = impl.greet(input.name)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[GrpcGreetingServiceOperation,GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing] = Greet
  }
  object Greet extends smithy4s.Endpoint[GrpcGreetingServiceOperation,GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing] {
    val schema: OperationSchema[GreetInput, GrpcGreetingServiceOperation.GreetError, GreetOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.grpc", "Greet"))
      .withInput(GreetInput.schema)
      .withError(GreetError.errorSchema)
      .withOutput(GreetOutput.schema)
    def wrap(input: GreetInput): Greet = Greet(input)
  }
  sealed trait GreetError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GreetError = this
    def $ordinal: Int

    object project {
      def notFoundError: Option[NotFoundError] = GreetError.NotFoundErrorCase.alt.project.lift(self).map(_.notFoundError)
      def permissionDeniedError: Option[PermissionDeniedError] = GreetError.PermissionDeniedErrorCase.alt.project.lift(self).map(_.permissionDeniedError)
    }

    def accept[A](visitor: GreetError.Visitor[A]): A = this match {
      case value: GreetError.NotFoundErrorCase => visitor.notFoundError(value.notFoundError)
      case value: GreetError.PermissionDeniedErrorCase => visitor.permissionDeniedError(value.permissionDeniedError)
    }
  }
  object GreetError extends ErrorSchema.Companion[GreetError] {

    def notFoundError(notFoundError: NotFoundError): GreetError = NotFoundErrorCase(notFoundError)
    def permissionDeniedError(permissionDeniedError: PermissionDeniedError): GreetError = PermissionDeniedErrorCase(permissionDeniedError)

    val id: ShapeId = ShapeId("smithy4s.example.grpc", "GreetError")

    val hints: Hints = Hints.empty

    final case class NotFoundErrorCase(notFoundError: NotFoundError) extends GreetError { final def $ordinal: Int = 0 }
    final case class PermissionDeniedErrorCase(permissionDeniedError: PermissionDeniedError) extends GreetError { final def $ordinal: Int = 1 }

    object NotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GreetError.NotFoundErrorCase] = bijection(NotFoundError.schema.addHints(hints), GreetError.NotFoundErrorCase(_), _.notFoundError)
      val alt = schema.oneOf[GreetError]("NotFoundError")
    }
    object PermissionDeniedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GreetError.PermissionDeniedErrorCase] = bijection(PermissionDeniedError.schema.addHints(hints), GreetError.PermissionDeniedErrorCase(_), _.permissionDeniedError)
      val alt = schema.oneOf[GreetError]("PermissionDeniedError")
    }

    trait Visitor[A] {
      def notFoundError(value: NotFoundError): A
      def permissionDeniedError(value: PermissionDeniedError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def notFoundError(value: NotFoundError): A = default
        def permissionDeniedError(value: PermissionDeniedError): A = default
      }
    }

    implicit val schema: Schema[GreetError] = union(
      GreetError.NotFoundErrorCase.alt,
      GreetError.PermissionDeniedErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GreetError] = throwable match {
      case e: NotFoundError => Some(GreetError.NotFoundErrorCase(e))
      case e: PermissionDeniedError => Some(GreetError.PermissionDeniedErrorCase(e))
      case _ => None
    }
    def unliftError(e: GreetError): Throwable = e match {
      case GreetError.NotFoundErrorCase(e) => e
      case GreetError.PermissionDeniedErrorCase(e) => e
    }
  }
}

