package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class XmlOutputInput(data: Option[String] = None)

object XmlOutputInput extends ShapeTag.Companion[XmlOutputInput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "XmlOutputInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(data: Option[String]): XmlOutputInput = XmlOutputInput(data)

  implicit val schema: Schema[XmlOutputInput] = struct(
    string.optional[XmlOutputInput]("data", _.data).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
