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

/** Mirrors the shape of AWS SageMaker (issue #1568): the endpoint prefix is
  * dotted and differs from the signing name, which matches the ARN namespace.
  */
trait DottedPrefixGen[F[_, _, _, _, _]] {
  self =>

  def doThing(): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing]

}

object DottedPrefixGen extends Service.Mixin[DottedPrefixGen, DottedPrefixOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.aws", "DottedPrefix")
  val version: String = ""

  val hints: Hints = Hints(
    aws.api.Service(sdkId = "DottedPrefix", arnNamespace = Some(aws.api.ArnNamespace("sagemaker")), cloudFormationName = None, cloudTrailEventSource = None, docId = None, endpointPrefix = Some("api.sagemaker"), cloudWatchNamespace = None),
    aws.auth.Sigv4(name = "sagemaker"),
    aws.protocols.AwsJson1_1(http = None, eventStreamHttp = None),
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("Mirrors the shape of AWS SageMaker (issue #1568): the endpoint prefix is\ndotted and differs from the signing name, which matches the ARN namespace.")),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[DottedPrefixOperation, _, _, _, _, _]] = Vector(
    DottedPrefixOperation.DoThing,
  )

  def input[I, E, O, SI, SO](op: DottedPrefixOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: DottedPrefixOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: DottedPrefixOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends DottedPrefixOperation.Transformed[DottedPrefixOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: DottedPrefixGen[DottedPrefixOperation] = DottedPrefixOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: DottedPrefixGen[P], f: PolyFunction5[P, P1]): DottedPrefixGen[P1] = new DottedPrefixOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[DottedPrefixOperation, P]): DottedPrefixGen[P] = new DottedPrefixOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: DottedPrefixGen[P]): PolyFunction5[DottedPrefixOperation, P] = DottedPrefixOperation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[DottedPrefixGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[DottedPrefixGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[DottedPrefixGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[DottedPrefixGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: DottedPrefixGen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[DottedPrefixGen[F]] = Transformation.of(alg)
  }
}

sealed trait DottedPrefixOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: DottedPrefixGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[DottedPrefixOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object DottedPrefixOperation {

  object reified extends DottedPrefixGen[DottedPrefixOperation] {
    def doThing(): DoThing = DoThing(DoThingInput())
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: DottedPrefixGen[P], f: PolyFunction5[P, P1]) extends DottedPrefixGen[P1] {
    def doThing(): P1[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = f[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing](this.alg.doThing())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: DottedPrefixGen[P]): PolyFunction5[DottedPrefixOperation, P] = new PolyFunction5[DottedPrefixOperation, P] {
    def apply[I, E, O, SI, SO](op: DottedPrefixOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class DoThing(input: DoThingInput) extends DottedPrefixOperation[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DottedPrefixGen[F]): F[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = impl.doThing()
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[DottedPrefixOperation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = DoThing
  }
  object DoThing extends smithy4s.Endpoint[DottedPrefixOperation,DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] {
    val schema: OperationSchema[DoThingInput, Nothing, DoThingOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example.aws", "DoThing"))
      .withInput(DoThingInput.schema)
      .withOutput(DoThingOutput.schema)
    def wrap(input: DoThingInput): DoThing = DoThing(input)
  }
}

