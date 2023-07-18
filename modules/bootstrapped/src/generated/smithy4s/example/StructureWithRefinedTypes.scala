package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedTypes(requiredAge: Age, age: Option[Age] = None, personAge: Option[PersonAge] = None, fancyList: Option[smithy4s.example.FancyList] = None, unwrappedFancyList: Option[smithy4s.refined.FancyList] = None, name: Option[smithy4s.example.Name] = None, dogName: Option[smithy4s.refined.Name] = None)
object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedTypes")

  val hints: Hints = Hints.empty

  object Lenses {
    val requiredAge = Lens[StructureWithRefinedTypes, Age](_.requiredAge)(n => a => a.copy(requiredAge = n))
    val age = Lens[StructureWithRefinedTypes, Option[Age]](_.age)(n => a => a.copy(age = n))
    val personAge = Lens[StructureWithRefinedTypes, Option[PersonAge]](_.personAge)(n => a => a.copy(personAge = n))
    val fancyList = Lens[StructureWithRefinedTypes, Option[smithy4s.example.FancyList]](_.fancyList)(n => a => a.copy(fancyList = n))
    val unwrappedFancyList = Lens[StructureWithRefinedTypes, Option[smithy4s.refined.FancyList]](_.unwrappedFancyList)(n => a => a.copy(unwrappedFancyList = n))
    val name = Lens[StructureWithRefinedTypes, Option[smithy4s.example.Name]](_.name)(n => a => a.copy(name = n))
    val dogName = Lens[StructureWithRefinedTypes, Option[smithy4s.refined.Name]](_.dogName)(n => a => a.copy(dogName = n))
  }

  implicit val schema: Schema[StructureWithRefinedTypes] = struct(
    Age.schema.required[StructureWithRefinedTypes]("requiredAge", _.requiredAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), smithy.api.Required()),
    Age.schema.optional[StructureWithRefinedTypes]("age", _.age).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    PersonAge.schema.optional[StructureWithRefinedTypes]("personAge", _.personAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    smithy4s.example.FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList),
    smithy4s.example.Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
    DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName),
  ){
    StructureWithRefinedTypes.apply
  }.withId(id).addHints(hints)
}
