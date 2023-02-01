package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example.refined.Age
import smithy4s.example.refined.Age.provider._
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

case class StructureWithRefinedTypes(age: Option[Age] = None, personAge: Option[Age] = None, fancyList: Option[smithy4s.example.FancyList] = None, unwrappedFancyList: Option[smithy4s.example.refined.FancyList] = None, name: Option[smithy4s.example.Name] = None, dogName: Option[smithy4s.example.refined.Name] = None)
object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedTypes")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[StructureWithRefinedTypes] = struct(
    int.refined[Age](smithy4s.example.AgeFormat()).optional[StructureWithRefinedTypes]("age", _.age).addHints(smithy4s.example.AgeFormat()),
    int.refined[Age](smithy4s.example.AgeFormat()).optional[StructureWithRefinedTypes]("personAge", _.personAge).addHints(smithy4s.example.AgeFormat()),
    smithy4s.example.FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList),
    smithy4s.example.Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
    DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName),
  ){
    StructureWithRefinedTypes.apply
  }.withId(id).addHints(hints)
}