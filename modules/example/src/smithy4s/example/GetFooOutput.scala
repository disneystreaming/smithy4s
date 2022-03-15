package smithy4s.example

import smithy4s.schema.syntax._

case class GetFooOutput(foo: Option[Foo] = None)
object GetFooOutput extends smithy4s.ShapeTag.Companion[GetFooOutput] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "GetFooOutput")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[GetFooOutput] = struct(
    Foo.schema.optional[GetFooOutput]("foo", _.foo),
  ){
    GetFooOutput.apply
  }.withId(id).addHints(hints)
}