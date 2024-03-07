package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultUnknownFieldRetentionExample(retainedUnknownFields: Document = smithy4s.Document.nullDoc, foo: Option[String] = None, bar: Option[String] = None)

object DefaultUnknownFieldRetentionExample extends ShapeTag.Companion[DefaultUnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultUnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(foo: Option[String], bar: Option[String], retainedUnknownFields: Document): DefaultUnknownFieldRetentionExample = DefaultUnknownFieldRetentionExample(retainedUnknownFields, foo, bar)

  implicit val schema: Schema[DefaultUnknownFieldRetentionExample] = struct(
    string.optional[DefaultUnknownFieldRetentionExample]("foo", _.foo),
    string.optional[DefaultUnknownFieldRetentionExample]("bar", _.bar),
    document.field[DefaultUnknownFieldRetentionExample]("retainedUnknownFields", _.retainedUnknownFields).addHints(alloy.UnknownDocumentFieldRetention(), smithy.api.Default(smithy4s.Document.nullDoc)),
  )(make).withId(id).addHints(hints)
}
