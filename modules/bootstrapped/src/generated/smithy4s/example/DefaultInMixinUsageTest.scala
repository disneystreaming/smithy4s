package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultInMixinUsageTest(one: String = "test") extends DefaultInMixinTest
object DefaultInMixinUsageTest extends ShapeTag.$Companion[DefaultInMixinUsageTest] {
  val $id: ShapeId = ShapeId("smithy4s.example", "DefaultInMixinUsageTest")

  val $hints: Hints = Hints.empty

  val one: FieldLens[DefaultInMixinUsageTest, String] = string.required[DefaultInMixinUsageTest]("one", _.one, n => c => c.copy(one = n)).addHints(Default(smithy4s.Document.fromString("test")))

  implicit val $schema: Schema[DefaultInMixinUsageTest] = struct(
    one,
  ){
    DefaultInMixinUsageTest.apply
  }.withId($id).addHints($hints)
}