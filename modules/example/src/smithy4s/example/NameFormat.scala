package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class NameFormat()
object NameFormat extends ShapeTag.Companion[NameFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nameFormat")
  
  val hints : Hints = Hints(
    smithy.api.Trait(Some("string"), None, None, None),
  )
  
  implicit val schema: Schema[NameFormat] = constant(NameFormat()).withId(id).addHints(hints)
}