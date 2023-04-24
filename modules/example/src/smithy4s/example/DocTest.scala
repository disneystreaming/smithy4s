package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** Test if an at-sign is rendered appropriately
  * {@literal @}test
  */
final case class DocTest()
object DocTest extends ShapeTag.Companion[DocTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DocTest")

  val hints: Hints = Hints(
    smithy.api.Documentation("Test if an at-sign is rendered appropriately\n@test"),
  )

  implicit val schema: Schema[DocTest] = constant(DocTest()).withId(id).addHints(hints)
}