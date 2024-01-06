package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class GetStreamedObjectOutput()

object GetStreamedObjectOutput extends ShapeTag.Companion[GetStreamedObjectOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObjectOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetStreamedObjectOutput] = constant(GetStreamedObjectOutput()).withId(id).addHints(hints)
}
