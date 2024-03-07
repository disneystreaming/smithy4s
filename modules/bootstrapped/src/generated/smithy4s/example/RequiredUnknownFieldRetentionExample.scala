package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RequiredUnknownFieldRetentionExample(retainedUnknownFields: Document, foo: Option[String] = None, bar: Option[String] = None)

object RequiredUnknownFieldRetentionExample extends ShapeTag.Companion[RequiredUnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "RequiredUnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(foo: Option[String], bar: Option[String], retainedUnknownFields: Document): RequiredUnknownFieldRetentionExample = RequiredUnknownFieldRetentionExample(retainedUnknownFields, foo, bar)

  implicit val schema: Schema[RequiredUnknownFieldRetentionExample] = struct(
    string.optional[RequiredUnknownFieldRetentionExample]("foo", _.foo),
    string.optional[RequiredUnknownFieldRetentionExample]("bar", _.bar),
    document.required[RequiredUnknownFieldRetentionExample]("retainedUnknownFields", _.retainedUnknownFields).addHints(alloy.UnknownDocumentFieldRetention(), alloy.UnknownJsonFieldRetention()),
  )(make).withId(id).addHints(hints)
}
