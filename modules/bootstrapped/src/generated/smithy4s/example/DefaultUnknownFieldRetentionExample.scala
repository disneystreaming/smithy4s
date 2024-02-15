package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultUnknownFieldRetentionExample(bazes: Map[String, Document] = Map(), foo: Option[String] = None, bar: Option[String] = None)

object DefaultUnknownFieldRetentionExample extends ShapeTag.Companion[DefaultUnknownFieldRetentionExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultUnknownFieldRetentionExample")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[DefaultUnknownFieldRetentionExample] = struct(
    RetainedUnknownFields.underlyingSchema.field[DefaultUnknownFieldRetentionExample]("bazes", _.bazes).addHints(alloy.UnknownFieldRetention(), smithy.api.Default(smithy4s.Document.obj())),
    string.optional[DefaultUnknownFieldRetentionExample]("foo", _.foo),
    string.optional[DefaultUnknownFieldRetentionExample]("bar", _.bar),
  ){
    DefaultUnknownFieldRetentionExample.apply
  }.withId(id).addHints(hints)
}
