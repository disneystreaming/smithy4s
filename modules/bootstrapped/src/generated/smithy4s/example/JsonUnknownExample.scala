package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class JsonUnknownExample(s: Option[String] = None, i: Option[Int] = None, additionalProperties: Option[Map[String, Document]] = None)

object JsonUnknownExample extends ShapeTag.Companion[JsonUnknownExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "JsonUnknownExample")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(s: Option[String], i: Option[Int], additionalProperties: Option[Map[String, Document]]): JsonUnknownExample = JsonUnknownExample(s, i, additionalProperties)

  implicit val schema: Schema[JsonUnknownExample] = struct(
    string.optional[JsonUnknownExample]("s", _.s),
    int.optional[JsonUnknownExample]("i", _.i),
    AdditionalProperties.underlyingSchema.optional[JsonUnknownExample]("additionalProperties", _.additionalProperties).addHints(alloy.JsonUnknown()),
  )(make).withId(id).addHints(hints)
}
