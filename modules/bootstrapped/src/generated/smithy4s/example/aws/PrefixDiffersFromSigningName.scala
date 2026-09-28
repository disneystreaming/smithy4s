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

/** Mirrors the shape of AWS SES v2: the endpoint prefix ("email"), the ARN
  * namespace ("ses") and the sigv4 signing name ("ses") are all different from
  * each other, so the host and the credential scope must be resolved
  * independently.
  */
trait PrefixDiffersFromSigningNameGen[F[_, _, _, _, _]] {
  self =>

  def doThing(): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing]

}

object PrefixDiffersFromSigningNameGen extends Service.Mixin[PrefixDiffersFromSigningNameGen, PrefixDiffersFromSigningNameOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.aws", "PrefixDiffersFromSigningName")
  val version: String = ""

  val hints: Hints = Hints(
    aws.api.Service(sdkId = "PrefixDiffersFromSigningName", arnNamespace = Some(aws.api.ArnNamespace("ses")), cloudFormationName = None, cloudTrailEventSource = None, docId = None, endpointPrefix = Some("email"), cloudWatchNamespace = None),
    aws.auth.Sigv4(name = "ses"),
    aws.protocols.AwsJson1_0(http = None, eventStreamHttp = None),
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Mirrors the shape of AWS SES v2: the endpoint prefix (\"email\"), the ARN\nnamespace (\"ses\") and the sigv4 signing name (\"ses\") are all different from\neach other, so the host and the credential scope must be resolved\nindependently.")),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[PrefixDiffersFromSigningNameOperation, _, _, _, _, _]] = Vector(
    PrefixDiffersFromSigningNameOperation.DoThing,
  )

  def input[I, E, O, SI, SO](op: PrefixDiffersFromSigningNameOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: PrefixDiffersFromSigningNameOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: PrefixDiffersFromSigningNameOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends PrefixDiffersFromSigningNameOperation.Transformed[PrefixDiffersFromSigningNameOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: PrefixDiffersFromSigningNameGen[PrefixDiffersFromSigningNameOperation] = PrefixDiffersFromSigningNameOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: PrefixDiffersFromSigningNameGen[P], f: PolyFunction5[P, P1]): PrefixDiffersFromSigningNameGen[P1] = new PrefixDiffersFromSigningNameOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[PrefixDiffersFromSigningNameOperation, P]): PrefixDiffersFromSigningNameGen[P] = new PrefixDiffersFromSigningNameOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: PrefixDiffersFromSigningNameGen[P]): PolyFunction5[PrefixDiffersFromSigningNameOperation, P] = PrefixDiffersFromSigningNameOperation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[PrefixDiffersFromSigningNameGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[PrefixDiffersFromSigningNameGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[PrefixDiffersFromSigningNameGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[PrefixDiffersFromSigningNameGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: PrefixDiffersFromSigningNameGen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[PrefixDiffersFromSigningNameGen[F]] = Transformation.of(alg)
  }
}

sealed trait PrefixDiffersFromSigningNameOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: PrefixDiffersFromSigningNameGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[PrefixDiffersFromSigningNameOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object PrefixDiffersFromSigningNameOperation {

  object reified extends PrefixDiffersFromSigningNameGen[PrefixDiffersFromSigningNameOperation] {
    def doThing(): DoThing = DoThing(DoThingInput())
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: PrefixDiffersFromSigningNameGen[P], f: PolyFunction5[P, P1]) extends PrefixDiffersFromSigningNameGen[P1] {
    def doThing(): P1[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = f[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing](this.alg.doThing())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: PrefixDiffersFromSigningNameGen[P]): PolyFunction5[PrefixDiffersFromSigningNameOperation, P] = new PolyFunction5[PrefixDiffersFromSigningNameOperation, P] {
    def apply[I, E, O, SI, SO](op: PrefixDiffersFromSigningNameOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DoThing(input: DoThingInput) extends PrefixDiffersFromSigningNameOperation[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PrefixDiffersFromSigningNameGen[F]): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = impl.doThing()
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[PrefixDiffersFromSigningNameOperation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = DoThing
  }
  object DoThing extends smithy4s.Endpoint[PrefixDiffersFromSigningNameOperation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    val schema: OperationSchema[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.aws", "DoThing"))
      .withInput(DoThingInput.schema)
      .withOutput(DoThingOutput.schema)
    def wrap(input: DoThingInput): DoThing = DoThing(input)
  }
}

