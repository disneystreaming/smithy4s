package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait DiscriminatedServiceGen[F[_, _, _, _, _]] {
  self =>

  def testDiscriminated(key: String): F[TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[DiscriminatedServiceGen[F]] = Transformation.of[DiscriminatedServiceGen[F]](this)
}

object DiscriminatedServiceGen extends Service.Mixin[DiscriminatedServiceGen, DiscriminatedServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "DiscriminatedService")
  val version: String = "1.0.0"

  val hints: Hints = Hints.lazily(
    Hints(
      alloy.SimpleRestJson(),
    )
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[DiscriminatedServiceOperation, _, _, _, _, _]] = Vector(
    DiscriminatedServiceOperation.TestDiscriminated,
  )

  def input[I, E, O, SI, SO](op: DiscriminatedServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: DiscriminatedServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: DiscriminatedServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends DiscriminatedServiceOperation.Transformed[DiscriminatedServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: DiscriminatedServiceGen[DiscriminatedServiceOperation] = DiscriminatedServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: DiscriminatedServiceGen[P], f: PolyFunction5[P, P1]): DiscriminatedServiceGen[P1] = new DiscriminatedServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[DiscriminatedServiceOperation, P]): DiscriminatedServiceGen[P] = new DiscriminatedServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: DiscriminatedServiceGen[P]): PolyFunction5[DiscriminatedServiceOperation, P] = DiscriminatedServiceOperation.toPolyFunction(impl)

}

sealed trait DiscriminatedServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: DiscriminatedServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[DiscriminatedServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object DiscriminatedServiceOperation {

  object reified extends DiscriminatedServiceGen[DiscriminatedServiceOperation] {
    def testDiscriminated(key: String): TestDiscriminated = TestDiscriminated(TestDiscriminatedInput(key))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: DiscriminatedServiceGen[P], f: PolyFunction5[P, P1]) extends DiscriminatedServiceGen[P1] {
    def testDiscriminated(key: String): P1[TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing] = f[TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing](alg.testDiscriminated(key))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: DiscriminatedServiceGen[P]): PolyFunction5[DiscriminatedServiceOperation, P] = new PolyFunction5[DiscriminatedServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: DiscriminatedServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class TestDiscriminated(input: TestDiscriminatedInput) extends DiscriminatedServiceOperation[TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DiscriminatedServiceGen[F]): F[TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing] = impl.testDiscriminated(input.key)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[DiscriminatedServiceOperation,TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing] = TestDiscriminated
  }
  object TestDiscriminated extends smithy4s.Endpoint[DiscriminatedServiceOperation,TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing] {
    val schema: OperationSchema[TestDiscriminatedInput, Nothing, TestDiscriminatedOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "TestDiscriminated"))
      .withInput(TestDiscriminatedInput.schema)
      .withOutput(TestDiscriminatedOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/test/{key}"), code = 200), smithy.api.Readonly())
    def wrap(input: TestDiscriminatedInput): TestDiscriminated = TestDiscriminated(input)
  }
}

