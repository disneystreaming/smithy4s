package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class StructureWithRefinedTypes(age: Option[Age]=None, personAge: Option[PersonAge]=None, fancyList: Option[smithy4s.example.FancyList]=None, unwrappedFancyList: Option[smithy4s.example.refined.FancyList]=None, name: Option[smithy4s.example.Name]=None, dogName: Option[smithy4s.example.refined.Name]=None)
object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedTypes")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[StructureWithRefinedTypes] = struct(
    Age.schema.optional[StructureWithRefinedTypes]("age", _.age),
    PersonAge.schema.optional[StructureWithRefinedTypes]("personAge", _.personAge),
    FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList),
    Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
    DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName),
  ){
    StructureWithRefinedTypes.apply
  }.withId(id).addHints(hints)
}