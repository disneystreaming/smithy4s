package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class DefaultTest(one: Int = 1, two: String = "test", three: List[String] = List())
object DefaultTest extends ShapeTag.Companion[DefaultTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultTest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultTest] = struct(
    int.required[DefaultTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0d))),
    string.required[DefaultTest]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
    StringList.underlyingSchema.required[DefaultTest]("three", _.three).addHints(smithy.api.Default(smithy4s.Document.array())),
  ){
    DefaultTest.apply
  }.withId(id).addHints(hints)
}