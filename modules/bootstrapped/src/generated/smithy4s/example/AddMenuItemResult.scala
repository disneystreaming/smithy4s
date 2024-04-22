package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class AddMenuItemResult(itemId: String, added: Timestamp)

object AddMenuItemResult extends ShapeTag.Companion[AddMenuItemResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemResult")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(itemId: String, added: Timestamp): AddMenuItemResult = AddMenuItemResult(itemId, added)

  implicit val schema: Schema[AddMenuItemResult] = struct(
    string.required[AddMenuItemResult]("itemId", _.itemId).addHints(smithy.api.HttpPayload()),
    timestamp.required[AddMenuItemResult]("added", _.added).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.HttpHeader("X-ADDED-AT")),
  )(make).withId(id).addHints(hints)
}
