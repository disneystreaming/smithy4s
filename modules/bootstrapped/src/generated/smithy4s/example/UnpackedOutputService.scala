package smithy4s.example

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
import smithy4s.schema.Schema.unit

trait UnpackedOutputServiceGen[F[_, _, _, _, _]] {
  self =>

  /** HTTP GET /optional */
  def getOptionalItem(): F[Unit, Nothing, Option[UnpackedItem], Nothing, Nothing]
  /** HTTP GET /required */
  def getRequiredItem(): F[Unit, Nothing, UnpackedItem, Nothing, Nothing]

}

object UnpackedOutputServiceGen extends Service.Mixin[UnpackedOutputServiceGen, UnpackedOutputServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "UnpackedOutputService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "simpleRestJson"), smithy4s.Document.obj()),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[UnpackedOutputServiceOperation, _, _, _, _, _]] = Vector(
    UnpackedOutputServiceOperation.GetOptionalItem,
    UnpackedOutputServiceOperation.GetRequiredItem,
  )

  def input[I, E, O, SI, SO](op: UnpackedOutputServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: UnpackedOutputServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: UnpackedOutputServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends UnpackedOutputServiceOperation.Transformed[UnpackedOutputServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: UnpackedOutputServiceGen[UnpackedOutputServiceOperation] = UnpackedOutputServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: UnpackedOutputServiceGen[P], f: PolyFunction5[P, P1]): UnpackedOutputServiceGen[P1] = new UnpackedOutputServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[UnpackedOutputServiceOperation, P]): UnpackedOutputServiceGen[P] = new UnpackedOutputServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: UnpackedOutputServiceGen[P]): PolyFunction5[UnpackedOutputServiceOperation, P] = UnpackedOutputServiceOperation.toPolyFunction(impl)


  implicit final class TransformFunctorOps[F[_]](private val alg: FunctorAlgebra[UnpackedOutputServiceGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[FunctorAlgebra[UnpackedOutputServiceGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformBifunctorOps[F[_, _]](private val alg: BiFunctorAlgebra[UnpackedOutputServiceGen, F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[BiFunctorAlgebra[UnpackedOutputServiceGen, F]] = Transformation.of(alg)
  }

  implicit final class TransformOps[F[_, _, _, _, _]](private val alg: UnpackedOutputServiceGen[F]) extends AnyVal {
    def transform: Transformation.PartiallyApplied[UnpackedOutputServiceGen[F]] = Transformation.of(alg)
  }
}

sealed trait UnpackedOutputServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: UnpackedOutputServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[UnpackedOutputServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object UnpackedOutputServiceOperation {

  object reified extends UnpackedOutputServiceGen[UnpackedOutputServiceOperation] {
    def getOptionalItem(): GetOptionalItem = GetOptionalItem()
    def getRequiredItem(): GetRequiredItem = GetRequiredItem()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: UnpackedOutputServiceGen[P], f: PolyFunction5[P, P1]) extends UnpackedOutputServiceGen[P1] {
    def getOptionalItem(): P1[Unit, Nothing, Option[UnpackedItem], Nothing, Nothing] = f[Unit, Nothing, Option[UnpackedItem], Nothing, Nothing](this.alg.getOptionalItem())
    def getRequiredItem(): P1[Unit, Nothing, UnpackedItem, Nothing, Nothing] = f[Unit, Nothing, UnpackedItem, Nothing, Nothing](this.alg.getRequiredItem())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: UnpackedOutputServiceGen[P]): PolyFunction5[UnpackedOutputServiceOperation, P] = new PolyFunction5[UnpackedOutputServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: UnpackedOutputServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetOptionalItem() extends UnpackedOutputServiceOperation[Unit, Nothing, Option[UnpackedItem], Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: UnpackedOutputServiceGen[F]): F[Unit, Nothing, Option[UnpackedItem], Nothing, Nothing] = impl.getOptionalItem()
    def ordinal: Int = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[UnpackedOutputServiceOperation,Unit, Nothing, Option[UnpackedItem], Nothing, Nothing] = GetOptionalItem
  }
  object GetOptionalItem extends smithy4s.Endpoint[UnpackedOutputServiceOperation,Unit, Nothing, Option[UnpackedItem], Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Option[UnpackedItem], Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetOptionalItem"))
      .withInput(unit)
      .withOutput(Schema.bijection(GetOptionalItemOutput.schema, (_: GetOptionalItemOutput).item, (x: Option[UnpackedItem]) => GetOptionalItemOutput(item = x)))
      .withHints(Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("GET"), "uri" -> smithy4s.Document.fromString("/optional"), "code" -> smithy4s.Document.fromLong(200L))), Hints.dynamic(ShapeId("smithy.api", "readonly"), smithy4s.Document.obj()))
    def wrap(input: Unit): GetOptionalItem = GetOptionalItem()
  }
  final case class GetRequiredItem() extends UnpackedOutputServiceOperation[Unit, Nothing, UnpackedItem, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: UnpackedOutputServiceGen[F]): F[Unit, Nothing, UnpackedItem, Nothing, Nothing] = impl.getRequiredItem()
    def ordinal: Int = 1
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[UnpackedOutputServiceOperation,Unit, Nothing, UnpackedItem, Nothing, Nothing] = GetRequiredItem
  }
  object GetRequiredItem extends smithy4s.Endpoint[UnpackedOutputServiceOperation,Unit, Nothing, UnpackedItem, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, UnpackedItem, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetRequiredItem"))
      .withInput(unit)
      .withOutput(Schema.bijection(GetRequiredItemOutput.schema, (_: GetRequiredItemOutput).item, (x: UnpackedItem) => GetRequiredItemOutput(item = x)))
      .withHints(Hints.dynamic(ShapeId("smithy.api", "http"), smithy4s.Document.obj("method" -> smithy4s.Document.fromString("GET"), "uri" -> smithy4s.Document.fromString("/required"), "code" -> smithy4s.Document.fromLong(200L))), Hints.dynamic(ShapeId("smithy.api", "readonly"), smithy4s.Document.obj()))
    def wrap(input: Unit): GetRequiredItem = GetRequiredItem()
  }
}

