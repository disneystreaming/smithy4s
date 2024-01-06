package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class DefaultInMixinUsageTest(one: String = "test") extends DefaultInMixinTest

object DefaultInMixinUsageTest extends ShapeTag.Companion[DefaultInMixinUsageTest] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultInMixinUsageTest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultInMixinUsageTest] = struct(
    string.field[DefaultInMixinUsageTest]("one", _.one).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("test"))),
  ){
    DefaultInMixinUsageTest.apply
  }.withId(id).addHints(hints)
}
