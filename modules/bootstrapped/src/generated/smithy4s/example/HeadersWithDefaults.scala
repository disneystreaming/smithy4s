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

  implicit val schema: Schema[HeadersWithDefaults] = struct(
    string.required[HeadersWithDefaults]("dflt", _.dflt).addHints(smithy.api.Default(smithy4s.Document.fromString("test")), smithy.api.HttpHeader("dflt")),
  ){
    HeadersWithDefaults.apply
  }.withId(id).addHints(hints)
}
