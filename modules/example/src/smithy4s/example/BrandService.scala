package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.unit
import smithy4s.Transformation
import smithy4s.Monadic
import smithy4s.Service
import smithy4s.Hints
import smithy4s.StreamingSchema
import smithy4s.ShapeId
import smithy4s.Endpoint

trait BrandServiceGen[F[_, _, _, _, _]] {
  self =>

  def addBrands(brands: Option[List[String]] = None) : F[AddBrandsInput, Nothing, Unit, Nothing, Nothing]

  def transform[G[_, _, _, _, _]](transformation : Transformation[F, G]) : BrandServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : Transformation[F, G]) extends BrandServiceGen[G] {
    def addBrands(brands: Option[List[String]] = None) = transformation[AddBrandsInput, Nothing, Unit, Nothing, Nothing](self.addBrands(brands))
  }
}

object BrandServiceGen extends Service[BrandServiceGen, BrandServiceOperation] {

  def apply[F[_]](implicit F: Monadic[BrandServiceGen, F]): F.type = F

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

  def transform[P[_, _, _, _, _]](transformation: Transformation[BrandServiceOperation, P]): BrandServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: BrandServiceGen[P], transformation: Transformation[P, P1]): BrandServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : BrandServiceGen[P]): Transformation[BrandServiceOperation, P] = new Transformation[BrandServiceOperation, P] {
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
