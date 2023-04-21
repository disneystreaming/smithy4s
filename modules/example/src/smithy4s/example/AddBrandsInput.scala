package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example.common.BrandList
import smithy4s.schema.Schema.struct

case class AddBrandsInput(brands: Option[List[String]] = None)
object AddBrandsInput extends ShapeTag.Companion[AddBrandsInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddBrandsInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AddBrandsInput] = struct(
    BrandList.underlyingSchema.optional[AddBrandsInput]("brands", _.brands),
  ){
    AddBrandsInput.apply
  }.withId(id).addHints(hints)
}
