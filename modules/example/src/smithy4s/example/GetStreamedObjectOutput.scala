package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

case class GetStreamedObjectOutput()
object GetStreamedObjectOutput extends ShapeTag.Companion[GetStreamedObjectOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObjectOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetStreamedObjectOutput] = constant(GetStreamedObjectOutput()).withId(id).addHints(hints)
}