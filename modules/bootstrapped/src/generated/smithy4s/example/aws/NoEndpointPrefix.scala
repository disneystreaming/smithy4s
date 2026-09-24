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

/** Mirrors the shape of the AWS Account API (issue #1532): no endpoint prefix
  * at all, so both the host and the signing name fall back to the ARN
  * namespace. Before the fix, the host was derived from the *operation* name.
  */
trait NoEndpointPrefixGen[F[_, _, _, _, _]] {
  self =>

  def doThing(): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing]

}

object NoEndpointPrefixGen extends Service.Mixin[NoEndpointPrefixGen, NoEndpointPrefixOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.aws", "NoEndpointPrefix")
  val version: String = ""

  val hints: Hints = Hints(
    aws.api.Service(sdkId = "NoEndpointPrefix", arnNamespace = Some(aws.api.ArnNamespace("account")), cloudFormationName = None, cloudTrailEventSource = None, docId = None, endpointPrefix = None, cloudWatchNamespace = None),
    aws.auth.Sigv4(name = "account"),
    aws.protocols.AwsJson1_0(http = None, eventStreamHttp = None),
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Mirrors the shape of the AWS Account API (issue #1532): no endpoint prefix\nat all, so both the host and the signing name fall back to the ARN\nnamespace. Before the fix, the host was derived from the *operation* name.")),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[NoEndpointPrefixOperation, _, _, _, _, _]] = Vector(
    NoEndpointPrefixOperation.DoThing,
  )

  def input[I, E, O, SI, SO](op: NoEndpointPrefixOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: NoEndpointPrefixOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: NoEndpointPrefixOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends NoEndpointPrefixOperation.Transformed[NoEndpointPrefixOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: NoEndpointPrefixGen[NoEndpointPrefixOperation] = NoEndpointPrefixOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: NoEndpointPrefixGen[P], f: PolyFunction5[P, P1]): NoEndpointPrefixGen[P1] = new NoEndpointPrefixOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[NoEndpointPrefixOperation, P]): NoEndpointPrefixGen[P] = new NoEndpointPrefixOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: NoEndpointPrefixGen[P]): PolyFunction5[NoEndpointPrefixOperation, P] = NoEndpointPrefixOperation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[NoEndpointPrefixGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[NoEndpointPrefixGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[NoEndpointPrefixGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[NoEndpointPrefixGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: NoEndpointPrefixGen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[NoEndpointPrefixGen[F]] = Transformation.of(alg)
  }
}

sealed trait NoEndpointPrefixOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: NoEndpointPrefixGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[NoEndpointPrefixOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object NoEndpointPrefixOperation {

  object reified extends NoEndpointPrefixGen[NoEndpointPrefixOperation] {
    def doThing(): DoThing = DoThing(DoThingInput())
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: NoEndpointPrefixGen[P], f: PolyFunction5[P, P1]) extends NoEndpointPrefixGen[P1] {
    def doThing(): P1[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = f[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing](this.alg.doThing())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: NoEndpointPrefixGen[P]): PolyFunction5[NoEndpointPrefixOperation, P] = new PolyFunction5[NoEndpointPrefixOperation, P] {
    def apply[I, E, O, SI, SO](op: NoEndpointPrefixOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DoThing(input: DoThingInput) extends NoEndpointPrefixOperation[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: NoEndpointPrefixGen[F]): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = impl.doThing()
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[NoEndpointPrefixOperation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = DoThing
  }
  object DoThing extends smithy4s.Endpoint[NoEndpointPrefixOperation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    val schema: OperationSchema[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.aws", "DoThing"))
      .withInput(DoThingInput.schema)
      .withOutput(DoThingOutput.schema)
    def wrap(input: DoThingInput): DoThing = DoThing(input)
  }
}

