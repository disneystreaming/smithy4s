package smithy4s.example

import smithy.api.Default
import smithy.api.HttpHeader
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HeadersWithDefaults(dflt: String = "test")
object HeadersWithDefaults extends ShapeTag.$Companion[HeadersWithDefaults] {
  val $id: ShapeId = ShapeId("smithy4s.example", "HeadersWithDefaults")

  val $hints: Hints = Hints.empty

  val dflt: FieldLens[HeadersWithDefaults, String] = string.required[HeadersWithDefaults]("dflt", _.dflt, n => c => c.copy(dflt = n)).addHints(Default(smithy4s.Document.fromString("test")), HttpHeader("dflt"))

  implicit val $schema: Schema[HeadersWithDefaults] = struct(
    dflt,
  ){
    HeadersWithDefaults.apply
  }.withId($id).addHints($hints)
}
