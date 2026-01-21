package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param data
  *   JSON payload type
  */
final case class JsonInputXmlOutputInput(data: Option[JsonPayload] = None)

object JsonInputXmlOutputInput extends ShapeTag.Companion[JsonInputXmlOutputInput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "JsonInputXmlOutputInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(data: Option[JsonPayload]): JsonInputXmlOutputInput = JsonInputXmlOutputInput(data)

  implicit val schema: Schema[JsonInputXmlOutputInput] = struct(
    JsonPayload.schema.optional[JsonInputXmlOutputInput]("data", _.data).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
