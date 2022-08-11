package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class GetStreamedObjectOutput()
object GetStreamedObjectOutput extends ShapeTag.Companion[GetStreamedObjectOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObjectOutput")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[GetStreamedObjectOutput] = constant(GetStreamedObjectOutput()).withId(id).addHints(hints)
}