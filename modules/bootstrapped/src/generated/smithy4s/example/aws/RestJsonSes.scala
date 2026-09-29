package smithy4s.example.aws

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.BiFunctorAlgebra
import smithy4s.kinds.FunctorAlgebra
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

/** Mirrors the shape of AWS SES v2 in full, protocol included: restJson1 routes
  * on the method and path, so a request must not carry the awsJson
  * `X-Amz-Target` header.
  */
trait RestJsonSesGen[F[_, _, _, _, _]] {
  self =>

  /** HTTP POST /v2/email/outbound-emails */
  def doRestThing(): F[DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing]

}

object RestJsonSesGen extends Service.Mixin[RestJsonSesGen, RestJsonSesOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.aws", "RestJsonSes")
  val version: String = ""

  val hints: Hints = Hints(
    aws.api.Service(sdkId = "RestJsonSes", arnNamespace = Some(aws.api.ArnNamespace("ses")), cloudFormationName = None, cloudTrailEventSource = None, docId = None, endpointPrefix = Some("email"), cloudWatchNamespace = None),
    aws.auth.Sigv4(name = "ses"),
    aws.protocols.RestJson1(http = None, eventStreamHttp = None),
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Mirrors the shape of AWS SES v2 in full, protocol included: restJson1 routes\non the method and path, so a request must not carry the awsJson\n`X-Amz-Target` header.")),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[RestJsonSesOperation, _, _, _, _, _]] = Vector(
    RestJsonSesOperation.DoRestThing,
  )

  def input[I, E, O, SI, SO](op: RestJsonSesOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: RestJsonSesOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: RestJsonSesOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends RestJsonSesOperation.Transformed[RestJsonSesOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: RestJsonSesGen[RestJsonSesOperation] = RestJsonSesOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: RestJsonSesGen[P], f: PolyFunction5[P, P1]): RestJsonSesGen[P1] = new RestJsonSesOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[RestJsonSesOperation, P]): RestJsonSesGen[P] = new RestJsonSesOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: RestJsonSesGen[P]): PolyFunction5[RestJsonSesOperation, P] = RestJsonSesOperation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[RestJsonSesGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[RestJsonSesGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[RestJsonSesGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[RestJsonSesGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: RestJsonSesGen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[RestJsonSesGen[F]] = Transformation.of(alg)
  }
}

sealed trait RestJsonSesOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: RestJsonSesGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[RestJsonSesOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object RestJsonSesOperation {

  object reified extends RestJsonSesGen[RestJsonSesOperation] {
    def doRestThing(): DoRestThing = DoRestThing(DoRestThingInput())
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: RestJsonSesGen[P], f: PolyFunction5[P, P1]) extends RestJsonSesGen[P1] {
    def doRestThing(): P1[DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing] = f[DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing](this.alg.doRestThing())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: RestJsonSesGen[P]): PolyFunction5[RestJsonSesOperation, P] = new PolyFunction5[RestJsonSesOperation, P] {
    def apply[I, E, O, SI, SO](op: RestJsonSesOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DoRestThing(input: DoRestThingInput) extends RestJsonSesOperation[DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: RestJsonSesGen[F]): F[DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing] = impl.doRestThing()
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[RestJsonSesOperation,DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing] = DoRestThing
  }
  object DoRestThing extends smithy4s.Endpoint[RestJsonSesOperation,DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing] {
    val schema: OperationSchema[DoRestThingInput, Nothing, DoRestThingOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.aws", "DoRestThing"))
      .withInput(DoRestThingInput.schema)
      .withOutput(DoRestThingOutput.schema)
      .withHints(Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("POST"), "uri" -> smithy4s.Document.fromString("/v2/email/outbound-emails"))))
    def wrap(input: DoRestThingInput): DoRestThing = DoRestThing(input)
  }
}

