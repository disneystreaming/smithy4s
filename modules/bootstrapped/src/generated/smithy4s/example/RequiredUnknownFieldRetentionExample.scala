package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RequiredUnknownFieldRetentionExample(bazes: Map[String, Document], foo: Option[String] = None, bar: Option[String] = None)

object RequiredUnknownFieldRetentionExample extends ShapeTag.Companion[RequiredUnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "RequiredUnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[RequiredUnknownFieldRetentionExample] = struct(
    RetainedUnknownFields.underlyingSchema.required[RequiredUnknownFieldRetentionExample]("bazes", _.bazes).addHints(alloy.UnknownFieldRetention()),
    string.optional[RequiredUnknownFieldRetentionExample]("foo", _.foo),
    string.optional[RequiredUnknownFieldRetentionExample]("bar", _.bar),
  ){
    RequiredUnknownFieldRetentionExample.apply
  }.withId(id).addHints(hints)
}
