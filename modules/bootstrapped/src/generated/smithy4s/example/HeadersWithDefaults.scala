package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HeadersWithDefaults(dflt: String = "test")

object HeadersWithDefaults extends ShapeTag.Companion[HeadersWithDefaults] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeadersWithDefaults")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(dflt: String): HeadersWithDefaults = HeadersWithDefaults(dflt)

  implicit val schema: Schema[HeadersWithDefaults] = struct(
    string.field[HeadersWithDefaults]("dflt", _.dflt).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("test")), Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("dflt"))),
  )(make).withId(id).addHints(hints)
}
