package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class FancyListFormat()

object FancyListFormat extends ShapeTag.Companion[FancyListFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "fancyListFormat")

  val hints: Hints = Hints(
    ShapeId("smithy.api", "trait") -> Document.obj("selector" -> Document.fromString("list:test(> member > string)")),
  )

  implicit val schema: Schema[FancyListFormat] = constant(FancyListFormat()).withId(id).addHints(hints)
}
