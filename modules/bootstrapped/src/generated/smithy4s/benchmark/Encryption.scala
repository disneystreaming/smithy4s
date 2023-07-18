package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Encryption(user: Option[String] = None, date: Option[Timestamp] = None, metadata: Option[EncryptionMetadata] = None)
object Encryption extends ShapeTag.Companion[Encryption] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Encryption")

  val hints: Hints = Hints.empty

  object Lenses {
    val user = Lens[Encryption, Option[String]](_.user)(n => a => a.copy(user = n))
    val date = Lens[Encryption, Option[Timestamp]](_.date)(n => a => a.copy(date = n))
    val metadata = Lens[Encryption, Option[EncryptionMetadata]](_.metadata)(n => a => a.copy(metadata = n))
  }

  implicit val schema: Schema[Encryption] = struct(
    string.optional[Encryption]("user", _.user),
    timestamp.optional[Encryption]("date", _.date).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    EncryptionMetadata.schema.optional[Encryption]("metadata", _.metadata),
  ){
    Encryption.apply
  }.withId(id).addHints(hints)
}
