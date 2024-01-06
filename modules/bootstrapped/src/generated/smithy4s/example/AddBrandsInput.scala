package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.example.common.BrandList

final case class AddBrandsInput(brands: Option[List[String]] = None)

object AddBrandsInput extends ShapeTag.Companion[AddBrandsInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddBrandsInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AddBrandsInput] = struct(
    BrandList.underlyingSchema.optional[AddBrandsInput]("brands", _.brands),
  ){
    AddBrandsInput.apply
  }.withId(id).addHints(hints)
}
