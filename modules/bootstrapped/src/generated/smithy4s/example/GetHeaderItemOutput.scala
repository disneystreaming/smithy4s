package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetHeaderItemOutput(itemId: String)

object GetHeaderItemOutput extends ShapeTag.Companion[GetHeaderItemOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetHeaderItemOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(itemId: String): GetHeaderItemOutput = GetHeaderItemOutput(itemId)

  implicit val schema: Schema[GetHeaderItemOutput] = struct[GetHeaderItemOutput](
    string.required[GetHeaderItemOutput]("itemId", _.itemId).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("X-Item-Id"))),
  )(make).withId(id).addHints(hints)
}
