package smithy4s.example

import smithy4s.schema.Schema._

case class NameFormat()
object NameFormat extends smithy4s.ShapeTag.Companion[NameFormat] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "nameFormat")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Trait(Some("string"), None, None, None),
  )

  implicit val schema: smithy4s.Schema[NameFormat] = constant(NameFormat()).withId(id).addHints(hints)
}