package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class XmlInputOutput(result: Option[String] = None)

object XmlInputOutput extends ShapeTag.Companion[XmlInputOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "XmlInputOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(result: Option[String]): XmlInputOutput = XmlInputOutput(result)

  implicit val schema: Schema[XmlInputOutput] = struct(
    string.optional[XmlInputOutput]("result", _.result).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
