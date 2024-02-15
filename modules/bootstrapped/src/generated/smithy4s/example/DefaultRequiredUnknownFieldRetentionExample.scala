package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultRequiredUnknownFieldRetentionExample(bazes: Map[String, Document] = Map(), foo: Option[String] = None, bar: Option[String] = None)

object DefaultRequiredUnknownFieldRetentionExample extends ShapeTag.Companion[DefaultRequiredUnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultRequiredUnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultRequiredUnknownFieldRetentionExample] = struct(
    RetainedUnknownFields.underlyingSchema.required[DefaultRequiredUnknownFieldRetentionExample]("bazes", _.bazes).addHints(alloy.UnknownFieldRetention(), smithy.api.Default(smithy4s.Document.obj())),
    string.optional[DefaultRequiredUnknownFieldRetentionExample]("foo", _.foo),
    string.optional[DefaultRequiredUnknownFieldRetentionExample]("bar", _.bar),
  ){
    DefaultRequiredUnknownFieldRetentionExample.apply
  }.withId(id).addHints(hints)
}
