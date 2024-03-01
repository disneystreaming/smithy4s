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

  // constructor using the original order from the spec
  private def make(user: Option[String], date: Option[Timestamp], metadata: Option[EncryptionMetadata]): Encryption = Encryption(user, date, metadata)

  implicit val schema: Schema[Encryption] = struct(
    string.optional[Encryption]("user", _.user),
    timestamp.optional[Encryption]("date", _.date).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    EncryptionMetadata.schema.optional[Encryption]("metadata", _.metadata),
  ){
    make
  }.withId(id).addHints(hints)
}
