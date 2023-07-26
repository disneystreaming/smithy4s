package smithy4s.example

import smithy.api.HttpHeader
import smithy.api.HttpPayload
import smithy.api.Required
import smithy.api.TimestampFormat
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

  val itemId = string.required[AddMenuItemResult]("itemId", _.itemId, n => c => c.copy(itemId = n)).addHints(HttpPayload(), Required())
  val added = timestamp.required[AddMenuItemResult]("added", _.added, n => c => c.copy(added = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen, Required(), HttpHeader("X-ADDED-AT"))

  implicit val schema: Schema[AddMenuItemResult] = struct(
    itemId,
    added,
  ){
    AddMenuItemResult.apply
  }
  .withId(ShapeId("smithy4s.example", "AddMenuItemResult"))
  .addHints(
    Hints.empty
  )
}
