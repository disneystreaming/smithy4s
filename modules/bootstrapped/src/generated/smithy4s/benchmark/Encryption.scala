package smithy4s.benchmark

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
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Encryption")

  val hints: Hints = Hints.empty

  val user = string.optional[Encryption]("user", _.user)
  val date = timestamp.optional[Encryption]("date", _.date).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen)
  val metadata = EncryptionMetadata.schema.optional[Encryption]("metadata", _.metadata)

  implicit val schema: Schema[Encryption] = struct(
    user,
    date,
    metadata,
  ){
    Encryption.apply
  }.withId(id).addHints(hints)
}
