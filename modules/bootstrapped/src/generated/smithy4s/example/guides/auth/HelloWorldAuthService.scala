package smithy4s.example.guides.auth

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

trait HelloWorldAuthServiceGen[F[_, _, _, _, _]] {
  self =>

  def sayWorld(): F[Unit, HelloWorldAuthServiceOperation.SayWorldError, World, Nothing, Nothing]
  def healthCheck(): F[Unit, HelloWorldAuthServiceOperation.HealthCheckError, HealthCheckOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[HelloWorldAuthServiceGen[F]] = Transformation.of[HelloWorldAuthServiceGen[F]](this)
}

object HelloWorldAuthServiceGen extends Service.Mixin[HelloWorldAuthServiceGen, HelloWorldAuthServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "HelloWorldAuthService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
    smithy.api.HttpBearerAuth(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[HelloWorldAuthServiceOperation, _, _, _, _, _]] = Vector(
    HelloWorldAuthServiceOperation.SayWorld,
    HelloWorldAuthServiceOperation.HealthCheck,
  )

  def input[I, E, O, SI, SO](op: HelloWorldAuthServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: HelloWorldAuthServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: HelloWorldAuthServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends HelloWorldAuthServiceOperation.Transformed[HelloWorldAuthServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: HelloWorldAuthServiceGen[HelloWorldAuthServiceOperation] = HelloWorldAuthServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: HelloWorldAuthServiceGen[P], f: PolyFunction5[P, P1]): HelloWorldAuthServiceGen[P1] = new HelloWorldAuthServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[HelloWorldAuthServiceOperation, P]): HelloWorldAuthServiceGen[P] = new HelloWorldAuthServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: HelloWorldAuthServiceGen[P]): PolyFunction5[HelloWorldAuthServiceOperation, P] = HelloWorldAuthServiceOperation.toPolyFunction(impl)

  type SayWorldError = HelloWorldAuthServiceOperation.SayWorldError
  val SayWorldError = HelloWorldAuthServiceOperation.SayWorldError
  type HealthCheckError = HelloWorldAuthServiceOperation.HealthCheckError
  val HealthCheckError = HelloWorldAuthServiceOperation.HealthCheckError
}

sealed trait HelloWorldAuthServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: HelloWorldAuthServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[HelloWorldAuthServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object HelloWorldAuthServiceOperation {

  object reified extends HelloWorldAuthServiceGen[HelloWorldAuthServiceOperation] {
    def sayWorld() = SayWorld()
    def healthCheck() = HealthCheck()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: HelloWorldAuthServiceGen[P], f: PolyFunction5[P, P1]) extends HelloWorldAuthServiceGen[P1] {
    def sayWorld() = f[Unit, HelloWorldAuthServiceOperation.SayWorldError, World, Nothing, Nothing](alg.sayWorld())
    def healthCheck() = f[Unit, HelloWorldAuthServiceOperation.HealthCheckError, HealthCheckOutput, Nothing, Nothing](alg.healthCheck())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: HelloWorldAuthServiceGen[P]): PolyFunction5[HelloWorldAuthServiceOperation, P] = new PolyFunction5[HelloWorldAuthServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: HelloWorldAuthServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class SayWorld() extends HelloWorldAuthServiceOperation[Unit, HelloWorldAuthServiceOperation.SayWorldError, World, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloWorldAuthServiceGen[F]): F[Unit, HelloWorldAuthServiceOperation.SayWorldError, World, Nothing, Nothing] = impl.sayWorld()
    def ordinal = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[HelloWorldAuthServiceOperation,Unit, HelloWorldAuthServiceOperation.SayWorldError, World, Nothing, Nothing] = SayWorld
  }
  object SayWorld extends smithy4s.Endpoint[HelloWorldAuthServiceOperation,Unit, HelloWorldAuthServiceOperation.SayWorldError, World, Nothing, Nothing] with Errorable[SayWorldError] {
    val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "SayWorld")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[World] = World.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/hello"), code = 200),
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = SayWorld()
    override val errorable: Option[Errorable[SayWorldError]] = Some(this)
    val error: UnionSchema[SayWorldError] = SayWorldError.schema
    def liftError(throwable: Throwable): Option[SayWorldError] = throwable match {
      case e: NotAuthorizedError => Some(SayWorldError.NotAuthorizedErrorCase(e))
      case _ => None
    }
    def unliftError(e: SayWorldError): Throwable = e match {
      case SayWorldError.NotAuthorizedErrorCase(e) => e
    }
  }
  sealed trait SayWorldError extends scala.Product with scala.Serializable {
    @inline final def widen: SayWorldError = this
    def _ordinal: Int
  }
  object SayWorldError extends ShapeTag.Companion[SayWorldError] {
    val hints: Hints = Hints.empty

    final case class NotAuthorizedErrorCase(notAuthorizedError: NotAuthorizedError) extends SayWorldError { final def _ordinal: Int = 0 }
    def notAuthorizedError(notAuthorizedError:NotAuthorizedError): SayWorldError = NotAuthorizedErrorCase(notAuthorizedError)

    object NotAuthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[NotAuthorizedErrorCase] = bijection(NotAuthorizedError.schema.addHints(hints), NotAuthorizedErrorCase(_), _.notAuthorizedError)
      val alt = schema.oneOf[SayWorldError]("NotAuthorizedError")
    }

    implicit val schema: UnionSchema[SayWorldError] = union(
      NotAuthorizedErrorCase.alt,
    ){
      _._ordinal
    }
  }
  final case class HealthCheck() extends HelloWorldAuthServiceOperation[Unit, HelloWorldAuthServiceOperation.HealthCheckError, HealthCheckOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: HelloWorldAuthServiceGen[F]): F[Unit, HelloWorldAuthServiceOperation.HealthCheckError, HealthCheckOutput, Nothing, Nothing] = impl.healthCheck()
    def ordinal = 1
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[HelloWorldAuthServiceOperation,Unit, HelloWorldAuthServiceOperation.HealthCheckError, HealthCheckOutput, Nothing, Nothing] = HealthCheck
  }
  object HealthCheck extends smithy4s.Endpoint[HelloWorldAuthServiceOperation,Unit, HelloWorldAuthServiceOperation.HealthCheckError, HealthCheckOutput, Nothing, Nothing] with Errorable[HealthCheckError] {
    val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "HealthCheck")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[HealthCheckOutput] = HealthCheckOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Auth(Set()),
      smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/health"), code = 200),
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = HealthCheck()
    override val errorable: Option[Errorable[HealthCheckError]] = Some(this)
    val error: UnionSchema[HealthCheckError] = HealthCheckError.schema
    def liftError(throwable: Throwable): Option[HealthCheckError] = throwable match {
      case e: NotAuthorizedError => Some(HealthCheckError.NotAuthorizedErrorCase(e))
      case _ => None
    }
    def unliftError(e: HealthCheckError): Throwable = e match {
      case HealthCheckError.NotAuthorizedErrorCase(e) => e
    }
  }
  sealed trait HealthCheckError extends scala.Product with scala.Serializable {
    @inline final def widen: HealthCheckError = this
    def _ordinal: Int
  }
  object HealthCheckError extends ShapeTag.Companion[HealthCheckError] {
    val hints: Hints = Hints.empty

    final case class NotAuthorizedErrorCase(notAuthorizedError: NotAuthorizedError) extends HealthCheckError { final def _ordinal: Int = 0 }
    def notAuthorizedError(notAuthorizedError:NotAuthorizedError): HealthCheckError = NotAuthorizedErrorCase(notAuthorizedError)

    object NotAuthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[NotAuthorizedErrorCase] = bijection(NotAuthorizedError.schema.addHints(hints), NotAuthorizedErrorCase(_), _.notAuthorizedError)
      val alt = schema.oneOf[HealthCheckError]("NotAuthorizedError")
    }

    implicit val schema: UnionSchema[HealthCheckError] = union(
      NotAuthorizedErrorCase.alt,
    ){
      _._ordinal
    }
  }
}

