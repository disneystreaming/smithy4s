package smithy4s.example

import smithy4s.schema.Schema._

case class DefaultTest(one: Int, two: String, three: List[String])
object DefaultTest extends smithy4s.ShapeTag.Companion[DefaultTest] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "DefaultTest")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[DefaultTest] = struct(
    int.required[DefaultTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0))),
    string.required[DefaultTest]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
    StringList.underlyingSchema.required[DefaultTest]("three", _.three).addHints(smithy.api.Default(smithy4s.Document.array())),
  ){
    DefaultTest.apply
  }.withId(id).addHints(hints)
}