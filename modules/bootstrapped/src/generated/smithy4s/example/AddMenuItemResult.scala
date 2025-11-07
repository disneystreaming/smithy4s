package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp
import smithy4s.time.Timestamp

final case class AddMenuItemResult(itemId: String, added: Timestamp)

object AddMenuItemResult extends ShapeTag.Companion[AddMenuItemResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemResult")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(itemId: String, added: Timestamp): AddMenuItemResult = AddMenuItemResult(itemId, added)

  implicit val schema: Schema[AddMenuItemResult] = struct(
    string.required[AddMenuItemResult]("itemId", _.itemId).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
    timestamp.required[AddMenuItemResult]("added", _.added).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("X-ADDED-AT")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("epoch-seconds"))),
  )(make).withId(id).addHints(hints)
}
