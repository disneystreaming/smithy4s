package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ShouldHaveDynamicBinding(a: Option[String] = None, b: Option[String] = None)

object ShouldHaveDynamicBinding extends ShapeTag.Companion[ShouldHaveDynamicBinding] {
  val id: ShapeId = ShapeId("smithy4s.example", "ShouldHaveDynamicBinding")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("1")),
    Hints.dynamic(ShapeId("smithy4s.example", "testDynamicBinding"), smithy4s.Document.obj("str" -> smithy4s.Document.fromString("test"))),
    Hints.dynamic(ShapeId("smithy4s.example.dynamic_traits", "thisWillBeDynamic"), smithy4s.Document.obj("test" -> smithy4s.Document.fromDouble(101.0d))),
  )

  // constructor using the original order from the spec
  private def make(a: Option[String], b: Option[String]): ShouldHaveDynamicBinding = ShouldHaveDynamicBinding(a, b)

  implicit val schema: Schema[ShouldHaveDynamicBinding] = struct(
    string.optional[ShouldHaveDynamicBinding]("a", _.a).addHints(Hints.dynamic(ShapeId("smithy.api", "since"), smithy4s.Document.fromString("2")), Hints.dynamic(ShapeId("smithy4s.example", "testDynamicBinding"), smithy4s.Document.obj("str" -> smithy4s.Document.fromString("test2"), "int" -> smithy4s.Document.fromDouble(1234.0d)))),
    string.validated(smithy.api.Length(min = Some(1L), max = None)).optional[ShouldHaveDynamicBinding]("b", _.b).addHints(Hints.dynamic(ShapeId("smithy4s.example", "testDynamicBinding"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
