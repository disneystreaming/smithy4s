package smithy4s.benchmark

import smithy.api.TimestampFormat
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Encryption(user: Option[String] = None, date: Option[Timestamp] = None, metadata: Option[EncryptionMetadata] = None)
object Encryption extends ShapeTag.Companion[Encryption] {

  val user = string.optional[Encryption]("user", _.user, n => c => c.copy(user = n))
  val date = timestamp.optional[Encryption]("date", _.date, n => c => c.copy(date = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen)
  val metadata = EncryptionMetadata.schema.optional[Encryption]("metadata", _.metadata, n => c => c.copy(metadata = n))

  implicit val schema: Schema[Encryption] = struct(
    user,
    date,
    metadata,
  ){
    Encryption.apply
  }
  .withId(ShapeId("smithy4s.benchmark", "Encryption"))
  .addHints(
    Hints.empty
  )
}
