package smithy4s.example

import smithy4s.example.common.BrandList.underlyingSchema
import smithy4s.schema.Schema._

case class AddBrandsInput(brands: Option[List[String]]=None)
object AddBrandsInput extends smithy4s.ShapeTag.Companion[AddBrandsInput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "AddBrandsInput")
  
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  
  implicit val schema: smithy4s.Schema[AddBrandsInput] = struct(
    BrandList.underlyingSchema.optional[AddBrandsInput]("brands", _.brands),
  ){
    AddBrandsInput.apply
  }.withId(id).addHints(hints)
}