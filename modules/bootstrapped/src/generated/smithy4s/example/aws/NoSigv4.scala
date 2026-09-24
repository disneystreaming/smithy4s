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

/** A service with no sigv4 trait, to check that the signing name falls back to
  * the ARN namespace rather than to the endpoint prefix.
  */
trait NoSigv4Gen[F[_, _, _, _, _]] {
  self =>

  def doThing(): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing]

}

object NoSigv4Gen extends Service.Mixin[NoSigv4Gen, NoSigv4Operation] {

  val id: ShapeId = ShapeId("smithy4s.example.aws", "NoSigv4")
  val version: String = ""

  val hints: Hints = Hints(
    aws.api.Service(sdkId = "NoSigv4", arnNamespace = Some(aws.api.ArnNamespace("arnnamespace")), cloudFormationName = None, cloudTrailEventSource = None, docId = None, endpointPrefix = Some("endpointprefix"), cloudWatchNamespace = None),
    aws.protocols.AwsJson1_0(http = None, eventStreamHttp = None),
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("A service with no sigv4 trait, to check that the signing name falls back to\nthe ARN namespace rather than to the endpoint prefix.")),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[NoSigv4Operation, _, _, _, _, _]] = Vector(
    NoSigv4Operation.DoThing,
  )

  def input[I, E, O, SI, SO](op: NoSigv4Operation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: NoSigv4Operation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: NoSigv4Operation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends NoSigv4Operation.Transformed[NoSigv4Operation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: NoSigv4Gen[NoSigv4Operation] = NoSigv4Operation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: NoSigv4Gen[P], f: PolyFunction5[P, P1]): NoSigv4Gen[P1] = new NoSigv4Operation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[NoSigv4Operation, P]): NoSigv4Gen[P] = new NoSigv4Operation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: NoSigv4Gen[P]): PolyFunction5[NoSigv4Operation, P] = NoSigv4Operation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[NoSigv4Gen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[NoSigv4Gen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[NoSigv4Gen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[NoSigv4Gen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: NoSigv4Gen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[NoSigv4Gen[F]] = Transformation.of(alg)
  }
}

sealed trait NoSigv4Operation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: NoSigv4Gen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[NoSigv4Operation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object NoSigv4Operation {

  object reified extends NoSigv4Gen[NoSigv4Operation] {
    def doThing(): DoThing = DoThing(DoThingInput())
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: NoSigv4Gen[P], f: PolyFunction5[P, P1]) extends NoSigv4Gen[P1] {
    def doThing(): P1[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = f[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing](this.alg.doThing())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: NoSigv4Gen[P]): PolyFunction5[NoSigv4Operation, P] = new PolyFunction5[NoSigv4Operation, P] {
    def apply[I, E, O, SI, SO](op: NoSigv4Operation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DoThing(input: DoThingInput) extends NoSigv4Operation[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: NoSigv4Gen[F]): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = impl.doThing()
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[NoSigv4Operation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = DoThing
  }
  object DoThing extends smithy4s.Endpoint[NoSigv4Operation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    val schema: OperationSchema[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.aws", "DoThing"))
      .withInput(DoThingInput.schema)
      .withOutput(DoThingOutput.schema)
    def wrap(input: DoThingInput): DoThing = DoThing(input)
  }
}

