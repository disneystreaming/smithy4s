package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultRequiredUnknownFieldRetentionExample(retainedUnknownFields: Document = smithy4s.Document.nullDoc, foo: Option[String] = None, bar: Option[String] = None)

object DefaultRequiredUnknownFieldRetentionExample extends ShapeTag.Companion[DefaultRequiredUnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultRequiredUnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(foo: Option[String], bar: Option[String], retainedUnknownFields: Document): DefaultRequiredUnknownFieldRetentionExample = DefaultRequiredUnknownFieldRetentionExample(retainedUnknownFields, foo, bar)

  implicit val schema: Schema[DefaultRequiredUnknownFieldRetentionExample] = struct(
    string.optional[DefaultRequiredUnknownFieldRetentionExample]("foo", _.foo),
    string.optional[DefaultRequiredUnknownFieldRetentionExample]("bar", _.bar),
    document.required[DefaultRequiredUnknownFieldRetentionExample]("retainedUnknownFields", _.retainedUnknownFields).addHints(smithy.api.Default(smithy4s.Document.nullDoc), alloy.UnknownJsonFieldRetention(), alloy.UnknownDocumentFieldRetention()),
  )(make).withId(id).addHints(hints)
}
