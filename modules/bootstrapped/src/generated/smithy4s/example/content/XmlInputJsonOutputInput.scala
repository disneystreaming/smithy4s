package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param data
  *   XML payload type
  */
final case class XmlInputJsonOutputInput(data: XmlPayload)

object XmlInputJsonOutputInput extends ShapeTag.Companion[XmlInputJsonOutputInput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "XmlInputJsonOutputInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: XmlPayload): XmlInputJsonOutputInput = XmlInputJsonOutputInput(data)

  implicit val schema: Schema[XmlInputJsonOutputInput] = struct(
    XmlPayload.schema.required[XmlInputJsonOutputInput]("data", _.data).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
