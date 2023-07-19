package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class QueriesWithDefaults(dflt: String = "test")
object QueriesWithDefaults extends ShapeTag.Companion[QueriesWithDefaults] {
  val id: ShapeId = ShapeId("smithy4s.example", "QueriesWithDefaults")

  val hints: Hints = Hints.empty

  object Optics {
    val dflt = Lens[QueriesWithDefaults, String](_.dflt)(n => a => a.copy(dflt = n))
  }

  implicit val schema: Schema[QueriesWithDefaults] = struct(
    string.required[QueriesWithDefaults]("dflt", _.dflt).addHints(smithy.api.Default(smithy4s.Document.fromString("test")), smithy.api.HttpQuery("dflt")),
  ){
    QueriesWithDefaults.apply
  }.withId(id).addHints(hints)
}
