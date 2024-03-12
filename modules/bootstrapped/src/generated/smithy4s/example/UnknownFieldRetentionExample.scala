package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnknownFieldRetentionExample(foo: Option[String] = None, bar: Option[String] = None, retainedUnknownFields: Option[Document] = None)

object UnknownFieldRetentionExample extends ShapeTag.Companion[UnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(foo: Option[String], bar: Option[String], retainedUnknownFields: Option[Document]): UnknownFieldRetentionExample = UnknownFieldRetentionExample(foo, bar, retainedUnknownFields)

  implicit val schema: Schema[UnknownFieldRetentionExample] = struct(
    string.optional[UnknownFieldRetentionExample]("foo", _.foo),
    string.optional[UnknownFieldRetentionExample]("bar", _.bar),
    document.optional[UnknownFieldRetentionExample]("retainedUnknownFields", _.retainedUnknownFields).addHints(alloy.UnknownDocumentFieldRetention(), alloy.UnknownJsonFieldRetention()),
  )(make).withId(id).addHints(hints)
}
