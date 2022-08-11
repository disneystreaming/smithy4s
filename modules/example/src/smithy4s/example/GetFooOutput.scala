package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class GetFooOutput(foo: Option[Foo]=None)
object GetFooOutput extends ShapeTag.Companion[GetFooOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetFooOutput")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[GetFooOutput] = struct(
    Foo.schema.optional[GetFooOutput]("foo", _.foo),
  ){
    GetFooOutput.apply
  }.withId(id).addHints(hints)
}