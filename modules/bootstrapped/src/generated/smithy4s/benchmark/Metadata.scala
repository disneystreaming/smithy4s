package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Metadata(contentType: Option[String] = None, lastModified: Option[Timestamp] = None, checkSum: Option[String] = None, pendingDeletion: Option[Boolean] = None, etag: Option[String] = None)

object Metadata extends ShapeTag.Companion[Metadata] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Metadata")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Metadata] = struct(
    string.optional[Metadata]("contentType", _.contentType),
    timestamp.optional[Metadata]("lastModified", _.lastModified).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    string.optional[Metadata]("checkSum", _.checkSum),
    boolean.optional[Metadata]("pendingDeletion", _.pendingDeletion),
    string.optional[Metadata]("etag", _.etag),
  ){
    Metadata.apply
  }.withId(id).addHints(hints)
}
