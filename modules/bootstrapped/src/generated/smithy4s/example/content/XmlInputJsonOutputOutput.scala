package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param result
  *   JSON payload type
  */
final case class XmlInputJsonOutputOutput(result: Option[JsonPayload] = None)

object XmlInputJsonOutputOutput extends ShapeTag.Companion[XmlInputJsonOutputOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "XmlInputJsonOutputOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(result: Option[JsonPayload]): XmlInputJsonOutputOutput = XmlInputJsonOutputOutput(result)

  implicit val schema: Schema[XmlInputJsonOutputOutput] = struct(
    JsonPayload.schema.optional[XmlInputJsonOutputOutput]("result", _.result).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
