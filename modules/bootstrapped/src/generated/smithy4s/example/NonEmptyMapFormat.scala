package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class NonEmptyMapFormat()

object NonEmptyMapFormat extends ShapeTag.Companion[NonEmptyMapFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nonEmptyMapFormat")

  val hints: Hints = Hints(
    ShapeId("smithy.api", "trait") -> Document.obj("selector" -> Document.fromString("map")),
  )

  implicit val schema: Schema[NonEmptyMapFormat] = constant(NonEmptyMapFormat()).withId(id).addHints(hints)
}
