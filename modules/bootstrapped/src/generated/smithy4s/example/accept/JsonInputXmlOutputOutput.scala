package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param result
  *   XML payload type
  */
final case class JsonInputXmlOutputOutput(result: Option[XmlPayload] = None)

object JsonInputXmlOutputOutput extends ShapeTag.Companion[JsonInputXmlOutputOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "JsonInputXmlOutputOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(result: Option[XmlPayload]): JsonInputXmlOutputOutput = JsonInputXmlOutputOutput(result)

  implicit val schema: Schema[JsonInputXmlOutputOutput] = struct(
    XmlPayload.schema.optional[JsonInputXmlOutputOutput]("result", _.result).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
