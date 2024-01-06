package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class QueriesWithDefaults(dflt: String = "test")

object QueriesWithDefaults extends ShapeTag.Companion[QueriesWithDefaults] {
  val id: ShapeId = ShapeId("smithy4s.example", "QueriesWithDefaults")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[QueriesWithDefaults] = struct(
    string.field[QueriesWithDefaults]("dflt", _.dflt).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("test")), smithy.api.HttpQuery("dflt")),
  ){
    QueriesWithDefaults.apply
  }.withId(id).addHints(hints)
}
