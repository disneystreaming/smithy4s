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
  val hints: Hints = Hints.empty

  val contentType = string.optional[Metadata]("contentType", _.contentType)
  val lastModified = timestamp.optional[Metadata]("lastModified", _.lastModified).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen)
  val checkSum = string.optional[Metadata]("checkSum", _.checkSum)
  val pendingDeletion = boolean.optional[Metadata]("pendingDeletion", _.pendingDeletion)
  val etag = string.optional[Metadata]("etag", _.etag)

  implicit val schema: Schema[Metadata] = struct(
    contentType,
    lastModified,
    checkSum,
    pendingDeletion,
    etag,
  ){
    Metadata.apply
  }.withId(ShapeId("smithy4s.benchmark", "Metadata")).addHints(hints)
}
