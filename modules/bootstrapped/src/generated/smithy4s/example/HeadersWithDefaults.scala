package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HeadersWithDefaults(dflt: String = "test")

object HeadersWithDefaults extends ShapeTag.Companion[HeadersWithDefaults] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeadersWithDefaults")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[HeadersWithDefaults] = struct(
    string.field[HeadersWithDefaults]("dflt", _.dflt).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("test")), smithy.api.HttpHeader("dflt")),
  ){
    HeadersWithDefaults.apply
  }.withId(id).addHints(hints)
}
