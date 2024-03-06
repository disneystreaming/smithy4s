package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedTypes(age: Age, personAge: PersonAge, requiredAge: Age, fancyList: Option[smithy4s.example.FancyList] = None, unwrappedFancyList: Option[smithy4s.refined.FancyList] = None, name: Option[smithy4s.example.Name] = None, dogName: Option[smithy4s.refined.Name] = None)

object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedTypes")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(age: Age, personAge: PersonAge, requiredAge: Age, fancyList: Option[smithy4s.example.FancyList], unwrappedFancyList: Option[smithy4s.refined.FancyList], name: Option[smithy4s.example.Name], dogName: Option[smithy4s.refined.Name]): StructureWithRefinedTypes = StructureWithRefinedTypes(age, personAge, requiredAge, fancyList, unwrappedFancyList, name, dogName)

  implicit val schema: Schema[StructureWithRefinedTypes] = struct(
    Age.schema.field[StructureWithRefinedTypes]("age", _.age).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    PersonAge.schema.field[StructureWithRefinedTypes]("personAge", _.personAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    Age.schema.required[StructureWithRefinedTypes]("requiredAge", _.requiredAge).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d))),
    smithy4s.example.FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList),
    smithy4s.example.Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
    DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName),
  )(make).withId(id).addHints(hints)
}
