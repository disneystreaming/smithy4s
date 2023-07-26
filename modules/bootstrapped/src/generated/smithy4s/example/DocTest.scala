package smithy4s.example

import smithy.api.Documentation
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

  implicit val schema: Schema[DocTest] = constant(DocTest()).withId(ShapeId("smithy4s.example", "DocTest"))
  .withId(ShapeId("smithy4s.example", "DocTest"))
  .addHints(
    Hints(
      Documentation("Test if an at-sign is rendered appropriately\n@test"),
    )
  )
}
