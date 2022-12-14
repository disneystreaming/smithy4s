package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class DefaultInMixinUsageTest(one: String = "test") extends DefaultInMixinTest
object DefaultInMixinUsageTest extends ShapeTag.Companion[DefaultInMixinUsageTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultInMixinUsageTest")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[DefaultInMixinUsageTest] = struct(
    string.required[DefaultInMixinUsageTest]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
  ){
    DefaultInMixinUsageTest.apply
  }.withId(id).addHints(hints)
}