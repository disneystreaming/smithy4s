package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedTypes(requiredAge: Age, age: Option[Age] = None, personAge: Option[PersonAge] = None, fancyList: Option[smithy4s.example.FancyList] = None, unwrappedFancyList: Option[smithy4s.refined.FancyList] = None, name: Option[smithy4s.example.Name] = None, dogName: Option[smithy4s.refined.Name] = None)
object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val hints: Hints = Hints.empty

  val requiredAge = Age.schema.required[StructureWithRefinedTypes]("requiredAge", _.requiredAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), smithy.api.Required())
  val age = Age.schema.optional[StructureWithRefinedTypes]("age", _.age).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val personAge = PersonAge.schema.optional[StructureWithRefinedTypes]("personAge", _.personAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)))
  val fancyList = smithy4s.example.FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList)
  val unwrappedFancyList = UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList)
  val name = smithy4s.example.Name.schema.optional[StructureWithRefinedTypes]("name", _.name)
  val dogName = DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName)

  implicit val schema: Schema[StructureWithRefinedTypes] = struct(
    requiredAge,
    age,
    personAge,
    fancyList,
    unwrappedFancyList,
    name,
    dogName,
  ){
    StructureWithRefinedTypes.apply
  }.withId(ShapeId("smithy4s.example", "StructureWithRefinedTypes")).addHints(hints)
}
