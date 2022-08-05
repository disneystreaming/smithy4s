package smithy4s.example

import smithy4s.example.AddBrandsInput.schema
import smithy4s.schema.Schema._

trait BrandServiceGen[F[_, _, _, _, _]] {
  self =>
  
  def addBrands(brands: Option[List[String]]=None) : F[AddBrandsInput, Nothing, Unit, Nothing, Nothing]
  
  def transform[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) : BrandServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) extends BrandServiceGen[G] {
    def addBrands(brands: Option[List[String]]=None) = transformation[AddBrandsInput, Nothing, Unit, Nothing, Nothing](self.addBrands(brands))
  }
}

object BrandServiceGen extends smithy4s.Service[BrandServiceGen, BrandServiceOperation] {
  
  def apply[F[_]](implicit F: smithy4s.Monadic[BrandServiceGen, F]): F.type = F
  
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "BrandService")
  
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  
  val endpoints: List[smithy4s.Endpoint[BrandServiceOperation, _, _, _, _, _]] = List(
    AddBrands,
  )
  
  val version: String = "1"
  
  def endpoint[I, E, O, SI, SO](op : BrandServiceOperation[I, E, O, SI, SO]) = op match {
    case AddBrands(input) => (input, AddBrands)
  }
  
  object reified extends BrandServiceGen[BrandServiceOperation] {
    def addBrands(brands: Option[List[String]]=None) = AddBrands(AddBrandsInput(brands))
  }
  
  def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[BrandServiceOperation, P]): BrandServiceGen[P] = reified.transform(transformation)
  
  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: BrandServiceGen[P], transformation: smithy4s.Transformation[P, P1]): BrandServiceGen[P1] = alg.transform(transformation)
  
  def asTransformation[P[_, _, _, _, _]](impl : BrandServiceGen[P]): smithy4s.Transformation[BrandServiceOperation, P] = new smithy4s.Transformation[BrandServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : BrandServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = op match  {
      case AddBrands(AddBrandsInput(brands)) => impl.addBrands(brands)
    }
  }
  case class AddBrands(input: AddBrandsInput) extends BrandServiceOperation[AddBrandsInput, Nothing, Unit, Nothing, Nothing]
  object AddBrands extends smithy4s.Endpoint[BrandServiceOperation, AddBrandsInput, Nothing, Unit, Nothing, Nothing] {
    val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "AddBrands")
    val input: smithy4s.Schema[AddBrandsInput] = AddBrandsInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: smithy4s.Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val streamedOutput : smithy4s.StreamingSchema[Nothing] = smithy4s.StreamingSchema.nothing
    val hints : smithy4s.Hints = smithy4s.Hints(
      smithy.api.Http(smithy.api.NonEmptyString("POST"), smithy.api.NonEmptyString("/brands"), Some(200)),
    )
    def wrap(input: AddBrandsInput) = AddBrands(input)
  }
  _
}

sealed trait BrandServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
