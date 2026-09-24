package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param data
  *   XML payload type
  */
final case class XmlInputInput(data: XmlPayload)

object XmlInputInput extends ShapeTag.Companion[XmlInputInput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "XmlInputInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: XmlPayload): XmlInputInput = XmlInputInput(data)

  implicit val schema: Schema[XmlInputInput] = struct(
    XmlPayload.schema.required[XmlInputInput]("data", _.data).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
