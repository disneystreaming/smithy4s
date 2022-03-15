package smithy4s.example

import smithy4s.syntax._

case class GetStreamedObjectOutput()
object GetStreamedObjectOutput extends smithy4s.ShapeTag.Companion[GetStreamedObjectOutput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetStreamedObjectOutput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[GetStreamedObjectOutput] = constant(GetStreamedObjectOutput()).withId(id).addHints(hints)
}