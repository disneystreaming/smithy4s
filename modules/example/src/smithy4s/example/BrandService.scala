package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.kinds.PolyFunction5
import smithy4s.Transformation
import smithy4s.kinds.FunctorAlgebra
import smithy4s.Service
import smithy4s.kinds.BiFunctorAlgebra
import smithy4s.Hints
import smithy4s.StreamingSchema
import smithy4s.ShapeId
import smithy4s.Endpoint

trait BrandServiceGen[F[_, _, _, _, _]] {
  self =>

  def addBrands(brands: Option[List[String]] = None) : F[AddBrandsInput, Nothing, Unit, Nothing, Nothing]

  def transform : Transformation.PartiallyApplied[BrandServiceGen[F]] = new Transformation.PartiallyApplied[BrandServiceGen[F]](this)
}

object BrandServiceGen extends Service.Mixin[BrandServiceGen, BrandServiceOperation] {

  def apply[F[_]](implicit F: FunctorAlgebra[BrandServiceGen, F]): F.type = F

  type WithError[F[_, _]] = BiFunctorAlgebra[BrandServiceGen, F]

  val id: ShapeId = ShapeId("smithy4s.example", "BrandService")

  val hints : Hints = Hints.empty

  val endpoints: List[Endpoint[BrandServiceOperation, _, _, _, _, _]] = List(
    AddBrands,
  )

  val version: String = "1"

  def endpoint[I, E, O, SI, SO](op : BrandServiceOperation[I, E, O, SI, SO]) = op match {
    case AddBrands(input) => (input, AddBrands)
  }

  object reified extends BrandServiceGen[BrandServiceOperation] {
    def addBrands(brands: Option[List[String]] = None) = AddBrands(AddBrandsInput(brands))
  }

  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: BrandServiceGen[P], f: PolyFunction5[P, P1]): BrandServiceGen[P1] = new Transformed(alg, f)

  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[BrandServiceOperation, P]): BrandServiceGen[P] = new Transformed(reified, f)
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: BrandServiceGen[P], f : PolyFunction5[P, P1]) extends BrandServiceGen[P1] {
    def addBrands(brands: Option[List[String]] = None) = f[AddBrandsInput, Nothing, Unit, Nothing, Nothing](alg.addBrands(brands))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl : BrandServiceGen[P]): PolyFunction5[BrandServiceOperation, P] = new PolyFunction5[BrandServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : BrandServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case AddBrands(AddBrandsInput(brands)) => impl.addBrands(brands)
    }
  }
  case class AddBrands(input: AddBrandsInput) extends BrandServiceOperation[AddBrandsInput, Nothing, Unit, Nothing, Nothing]
  object AddBrands extends Endpoint[BrandServiceOperation, AddBrandsInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "AddBrands")
    val input: Schema[AddBrandsInput] = AddBrandsInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput : StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints : Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/brands"), code = 200),
    )
    def wrap(input: AddBrandsInput) = AddBrands(input)
  }
}

sealed trait BrandServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
