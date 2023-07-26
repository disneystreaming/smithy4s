package smithy4s.example

import smithy.api.Http
import smithy.api.NonEmptyString
import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.unit

trait BrandServiceGen[F[_, _, _, _, _]] {
  self =>

  def addBrands(brands: Option[List[String]] = None): F[AddBrandsInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[BrandServiceGen[F]] = Transformation.of[BrandServiceGen[F]](this)
}

object BrandServiceGen extends Service.Mixin[BrandServiceGen, BrandServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "BrandService")
  val version: String = "1"

  val hints: Hints =
  Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[BrandServiceOperation, _, _, _, _, _]] = Vector(
    BrandServiceOperation.AddBrands,
  )

  def input[I, E, O, SI, SO](op: BrandServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: BrandServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: BrandServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends BrandServiceOperation.Transformed[BrandServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: BrandServiceGen[BrandServiceOperation] = BrandServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: BrandServiceGen[P], f: PolyFunction5[P, P1]): BrandServiceGen[P1] = new BrandServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[BrandServiceOperation, P]): BrandServiceGen[P] = new BrandServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: BrandServiceGen[P]): PolyFunction5[BrandServiceOperation, P] = BrandServiceOperation.toPolyFunction(impl)

}

sealed trait BrandServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: BrandServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[BrandServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object BrandServiceOperation {

  object reified extends BrandServiceGen[BrandServiceOperation] {
    def addBrands(brands: Option[List[String]] = None) = AddBrands(AddBrandsInput(brands))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: BrandServiceGen[P], f: PolyFunction5[P, P1]) extends BrandServiceGen[P1] {
    def addBrands(brands: Option[List[String]] = None) = f[AddBrandsInput, Nothing, Unit, Nothing, Nothing](alg.addBrands(brands))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: BrandServiceGen[P]): PolyFunction5[BrandServiceOperation, P] = new PolyFunction5[BrandServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: BrandServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class AddBrands(input: AddBrandsInput) extends BrandServiceOperation[AddBrandsInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: BrandServiceGen[F]): F[AddBrandsInput, Nothing, Unit, Nothing, Nothing] = impl.addBrands(input.brands)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[BrandServiceOperation,AddBrandsInput, Nothing, Unit, Nothing, Nothing] = AddBrands
  }
  object AddBrands extends smithy4s.Endpoint[BrandServiceOperation,AddBrandsInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "AddBrands")
    val input: Schema[AddBrandsInput] = AddBrandsInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/brands"), code = 200),
    )
    def wrap(input: AddBrandsInput) = AddBrands(input)
    override val errorable: Option[Nothing] = None
  }
}

