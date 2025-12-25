package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class ShouldHaveDynamicBindingTwo()

object ShouldHaveDynamicBindingTwo extends ShapeTag.Companion[ShouldHaveDynamicBindingTwo] {
  val id: ShapeId = ShapeId("smithy4s.example", "ShouldHaveDynamicBindingTwo")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy4s.example", "testDynamicBinding"), smithy4s.Document.obj("str" -> smithy4s.Document.fromString("test"))),
  )


  implicit val schema: Schema[ShouldHaveDynamicBindingTwo] = constant(ShouldHaveDynamicBindingTwo()).withId(id).addHints(hints)
}
