package smithy4s.example

import smithy4s.schema.Schema._

case class AgeFormat()
object AgeFormat extends smithy4s.ShapeTag.Companion[AgeFormat] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ageFormat")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Trait(Some("integer"), None, None),
  )

  implicit val schema: smithy4s.Schema[AgeFormat] = constant(AgeFormat()).withId(id).addHints(hints)
}