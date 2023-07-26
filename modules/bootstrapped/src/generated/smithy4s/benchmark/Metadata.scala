package smithy4s.benchmark

import smithy.api.TimestampFormat
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Metadata(contentType: Option[String] = None, lastModified: Option[Timestamp] = None, checkSum: Option[String] = None, pendingDeletion: Option[Boolean] = None, etag: Option[String] = None)
object Metadata extends ShapeTag.Companion[Metadata] {

  val contentType: FieldLens[Metadata, Option[String]] = string.optional[Metadata]("contentType", _.contentType, n => c => c.copy(contentType = n))
  val lastModified: FieldLens[Metadata, Option[Timestamp]] = timestamp.optional[Metadata]("lastModified", _.lastModified, n => c => c.copy(lastModified = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen)
  val checkSum: FieldLens[Metadata, Option[String]] = string.optional[Metadata]("checkSum", _.checkSum, n => c => c.copy(checkSum = n))
  val pendingDeletion: FieldLens[Metadata, Option[Boolean]] = boolean.optional[Metadata]("pendingDeletion", _.pendingDeletion, n => c => c.copy(pendingDeletion = n))
  val etag: FieldLens[Metadata, Option[String]] = string.optional[Metadata]("etag", _.etag, n => c => c.copy(etag = n))

  implicit val schema: Schema[Metadata] = struct(
    contentType,
    lastModified,
    checkSum,
    pendingDeletion,
    etag,
  ){
    Metadata.apply
  }
  .withId(ShapeId("smithy4s.benchmark", "Metadata"))
}
