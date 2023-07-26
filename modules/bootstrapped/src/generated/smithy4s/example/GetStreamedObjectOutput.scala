package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class GetStreamedObjectOutput()
object GetStreamedObjectOutput extends ShapeTag.Companion[GetStreamedObjectOutput] {

  implicit val schema: Schema[GetStreamedObjectOutput] = constant(GetStreamedObjectOutput()).withId(ShapeId("smithy4s.example", "GetStreamedObjectOutput"))
  .withId(ShapeId("smithy4s.example", "GetStreamedObjectOutput"))
  .addHints(
    Hints.empty
  )
}
