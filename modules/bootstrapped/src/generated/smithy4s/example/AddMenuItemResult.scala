package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class AddMenuItemResult(itemId: String, added: Timestamp)
object AddMenuItemResult extends ShapeTag.Companion[AddMenuItemResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemResult")

  val hints: Hints = Hints.empty

  object Lenses {
    val itemId = Lens[AddMenuItemResult, String](_.itemId)(n => a => a.copy(itemId = n))
    val added = Lens[AddMenuItemResult, Timestamp](_.added)(n => a => a.copy(added = n))
  }

  implicit val schema: Schema[AddMenuItemResult] = struct(
    string.required[AddMenuItemResult]("itemId", _.itemId).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
    timestamp.required[AddMenuItemResult]("added", _.added).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.Required(), smithy.api.HttpHeader("X-ADDED-AT")),
  ){
    AddMenuItemResult.apply
  }.withId(id).addHints(hints)
}
