package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Timestamp
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.timestamp

final case class AddMenuItemResult(itemId: String, added: Timestamp)

object AddMenuItemResult extends ShapeTag.Companion[AddMenuItemResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemResult")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AddMenuItemResult] = struct(
    string.required[AddMenuItemResult]("itemId", _.itemId).addHints(smithy.api.HttpPayload()),
    timestamp.required[AddMenuItemResult]("added", _.added).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.HttpHeader("X-ADDED-AT")),
  ){
    AddMenuItemResult.apply
  }.withId(id).addHints(hints)
}
