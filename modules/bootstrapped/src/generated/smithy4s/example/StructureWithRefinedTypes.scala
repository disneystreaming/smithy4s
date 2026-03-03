package smithy4s.example

import java.util.UUID
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.uuid

final case class StructureWithRefinedTypes(requiredAge: smithy4s.example.Age, personAge: PersonAge, inlineFieldConstraint: smithy4s.refined.Age, uuidField: UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"), age: Option[smithy4s.example.Age] = None, fancyList: Option[smithy4s.example.FancyList] = None, unwrappedFancyList: Option[smithy4s.refined.FancyList] = None, name: Option[smithy4s.example.Name] = None, dogName: Option[smithy4s.refined.Name] = None)

object StructureWithRefinedTypes extends ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedTypes")

  val hints: Hints = Hints(
    smithy4s.example.UuidTrait(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")),
  ).lazily

  // constructor using the original order from the spec
  private def make(age: Option[smithy4s.example.Age], personAge: PersonAge, requiredAge: smithy4s.example.Age, fancyList: Option[smithy4s.example.FancyList], unwrappedFancyList: Option[smithy4s.refined.FancyList], name: Option[smithy4s.example.Name], dogName: Option[smithy4s.refined.Name], inlineFieldConstraint: smithy4s.refined.Age, uuidField: UUID): StructureWithRefinedTypes = StructureWithRefinedTypes(requiredAge, personAge, inlineFieldConstraint, uuidField, age, fancyList, unwrappedFancyList, name, dogName)

  implicit val schema: Schema[StructureWithRefinedTypes] = struct[StructureWithRefinedTypes](
    smithy4s.example.Age.schema.optional[StructureWithRefinedTypes]("age", _.age),
    PersonAge.schema.field[StructureWithRefinedTypes]("personAge", _.personAge).addHints(smithy.api.Default(smithy4s.Document.fromLong(1L))),
    smithy4s.example.Age.schema.required[StructureWithRefinedTypes]("requiredAge", _.requiredAge),
    smithy4s.example.FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    UnwrappedFancyList.underlyingSchema.optional[StructureWithRefinedTypes]("unwrappedFancyList", _.unwrappedFancyList),
    smithy4s.example.Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
    DogName.underlyingSchema.optional[StructureWithRefinedTypes]("dogName", _.dogName),
    int.refined[smithy4s.refined.Age](smithy4s.example.AgeFormat()).field[StructureWithRefinedTypes]("inlineFieldConstraint", _.inlineFieldConstraint).addHints(smithy.api.Default(smithy4s.Document.fromLong(1L))),
    uuid.field[StructureWithRefinedTypes]("uuidField", _.uuidField).addHints(smithy.api.Default(smithy4s.Document.fromString("00000000-0000-0000-0000-000000000000"))),
  )(make).withId(id).addHints(hints)
}
