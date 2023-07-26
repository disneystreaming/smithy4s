package smithy4s.benchmark

import smithy.api.TimestampFormat
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Encryption(user: Option[String] = None, date: Option[Timestamp] = None, metadata: Option[EncryptionMetadata] = None)
object Encryption extends ShapeTag.$Companion[Encryption] {
  val $id: ShapeId = ShapeId("smithy4s.benchmark", "Encryption")

  val $hints: Hints = Hints.empty

  val user: FieldLens[Encryption, Option[String]] = string.optional[Encryption]("user", _.user, n => c => c.copy(user = n))
  val date: FieldLens[Encryption, Option[Timestamp]] = timestamp.optional[Encryption]("date", _.date, n => c => c.copy(date = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen)
  val metadata: FieldLens[Encryption, Option[EncryptionMetadata]] = EncryptionMetadata.$schema.optional[Encryption]("metadata", _.metadata, n => c => c.copy(metadata = n))

  implicit val $schema: Schema[Encryption] = struct(
    user,
    date,
    metadata,
  ){
    Encryption.apply
  }.withId($id).addHints($hints)
}
