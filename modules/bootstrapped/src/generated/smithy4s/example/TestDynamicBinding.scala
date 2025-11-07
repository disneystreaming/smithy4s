package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestDynamicBinding(str: Option[String] = None, int: Option[Int] = None)

object TestDynamicBinding extends ShapeTag.Companion[TestDynamicBinding] {
  val id: ShapeId = ShapeId("smithy4s.example", "testDynamicBinding")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "trait"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(str: Option[String], int: Option[Int]): TestDynamicBinding = TestDynamicBinding(str, int)

  implicit val schema: Schema[TestDynamicBinding] = recursive(struct(
    string.optional[TestDynamicBinding]("str", _.str),
    int.optional[TestDynamicBinding]("int", _.int),
  )(make).withId(id).addHints(hints))
}
