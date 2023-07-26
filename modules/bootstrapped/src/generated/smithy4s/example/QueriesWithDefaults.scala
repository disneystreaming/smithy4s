package smithy4s.example

import smithy.api.Default
import smithy.api.HttpQuery
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class QueriesWithDefaults(dflt: String = "test")
object QueriesWithDefaults extends ShapeTag.Companion[QueriesWithDefaults] {

  val dflt: FieldLens[QueriesWithDefaults, String] = string.required[QueriesWithDefaults]("dflt", _.dflt, n => c => c.copy(dflt = n)).addHints(Default(smithy4s.Document.fromString("test")), HttpQuery("dflt"))

  implicit val schema: Schema[QueriesWithDefaults] = struct(
    dflt,
  ){
    QueriesWithDefaults.apply
  }
  .withId(ShapeId("smithy4s.example", "QueriesWithDefaults"))
}
