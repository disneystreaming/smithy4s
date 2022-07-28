package smithy4s.example

import smithy4s.schema.Schema._

case class StructureWithRefinedTypes(age: Option[Age] = None, personAge: Option[PersonAge] = None, fancyList: Option[FancyList] = None, name: Option[Name] = None)
object StructureWithRefinedTypes extends smithy4s.ShapeTag.Companion[StructureWithRefinedTypes] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "StructureWithRefinedTypes")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[StructureWithRefinedTypes] = struct(
    Age.schema.optional[StructureWithRefinedTypes]("age", _.age),
    PersonAge.schema.optional[StructureWithRefinedTypes]("personAge", _.personAge),
    FancyList.schema.optional[StructureWithRefinedTypes]("fancyList", _.fancyList),
    Name.schema.optional[StructureWithRefinedTypes]("name", _.name),
  ){
    StructureWithRefinedTypes.apply
  }.withId(id).addHints(hints)
}