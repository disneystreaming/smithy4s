package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class AgeFormat()
object AgeFormat extends ShapeTag.Companion[AgeFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "ageFormat")
  
  val hints : Hints = Hints(
    smithy.api.Trait(Some("integer"), None, None, None),
  )
  
  implicit val schema: Schema[AgeFormat] = constant(AgeFormat()).withId(id).addHints(hints)
}