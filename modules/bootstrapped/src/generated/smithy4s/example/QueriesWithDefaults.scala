package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class QueriesWithDefaults(dflt: String = "test")

object QueriesWithDefaults extends ShapeTag.Companion[QueriesWithDefaults] {
  val id: ShapeId = ShapeId("smithy4s.example", "QueriesWithDefaults")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(dflt: String): QueriesWithDefaults = QueriesWithDefaults(dflt)

  implicit val schema: Schema[QueriesWithDefaults] = struct(
    string.field[QueriesWithDefaults]("dflt", _.dflt).addHints(smithy.api.Default(smithy4s.Document.fromString("test")), smithy.api.HttpQuery("dflt")),
  )(make).withId(id).addHints(hints)
}
