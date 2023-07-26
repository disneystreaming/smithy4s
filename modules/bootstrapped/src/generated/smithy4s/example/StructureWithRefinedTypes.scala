package smithy4s.example

import smithy.api.Default
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedTypes(requiredAge: Age, age: Option[Age] = None, personAge: Option[PersonAge] = None, fancyList: Option[smithy4s.example.FancyList] = None, unwrappedFancyList: Option[smithy4s.refined.FancyList] = None, name: Option[smithy4s.example.Name] = None, dogName: Option[smithy4s.refined.Name] = None)
object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {

  val requiredAge: FieldLens[StructureWithRefinedTypes, Age] = Age.schema.required[StructureWithRefinedTypes]("requiredAge", _.requiredAge, n => c => c.copy(requiredAge = n)).addHints(Default(smithy4s.Document.fromDouble(0.0d)), Required())
  val age: FieldLens[StructureWithRefinedTypes, Option[Age]] = Age.schema.optional[StructureWithRefinedTypes]("age", _.age, n => c => c.copy(age = n)).addHints(Default(smithy4s.Document.fromDouble(0.0d)))
  val personAge: FieldLens[StructureWithRefinedTypes, Option[PersonAge]] = PersonAge.schema.optional[StructureWithRefinedTypes]("personAge", _.personAge, n => c => c.copy(personAge = n)).addHints(Default(smithy4s.Document.fromDouble(0.0d)))
  val fancyList: FieldLens[StructureWithRefinedTypes, Option[smithy4s.example.FancyList]] = smithy4s.example.FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList, n => c => c.copy(fancyList = n))
  val unwrappedFancyList: FieldLens[StructureWithRefinedTypes, Option[smithy4s.refined.FancyList]] = UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList, n => c => c.copy(unwrappedFancyList = n))
  val name: FieldLens[StructureWithRefinedTypes, Option[smithy4s.example.Name]] = smithy4s.example.Name.schema.optional[StructureWithRefinedTypes]("name", _.name, n => c => c.copy(name = n))
  val dogName: FieldLens[StructureWithRefinedTypes, Option[smithy4s.refined.Name]] = DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName, n => c => c.copy(dogName = n))

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
  }
  .withId(ShapeId("smithy4s.example", "StructureWithRefinedTypes"))
}
