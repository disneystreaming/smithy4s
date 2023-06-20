package smithy4s.example

import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.struct

final case class BlobBody(blob: ByteArray)
object BlobBody extends ShapeTag.Companion[BlobBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "BlobBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[BlobBody] = struct(
    bytes.required[BlobBody]("blob", _.blob).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    BlobBody.apply
  }.withId(id).addHints(hints)
}
