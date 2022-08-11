package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class StructureWithRefinedTypes(age: Option[Age]=None, personAge: Option[PersonAge]=None, fancyList: Option[smithy4s.example.FancyList]=None, unwrappedFancyList: Option[smithy4s.example.refined.FancyList]=None, name: Option[smithy4s.example.Name]=None, dogName: Option[smithy4s.example.refined.Name]=None)
object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedTypes")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[StructureWithRefinedTypes] = struct(
    Age.schema.optional[StructureWithRefinedTypes]("age", _.age).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0))),
    PersonAge.schema.optional[StructureWithRefinedTypes]("personAge", _.personAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0))),
    FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList),
    Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
    DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName),
  ){
    StructureWithRefinedTypes.apply
  }.withId(id).addHints(hints)
}