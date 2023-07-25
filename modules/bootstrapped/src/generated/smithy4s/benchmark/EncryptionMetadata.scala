package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EncryptionMetadata(system: Option[String] = None, credentials: Option[Creds] = None, partial: Option[Boolean] = None)
object EncryptionMetadata extends ShapeTag.Companion[EncryptionMetadata] {
  val hints: Hints = Hints.empty

  val system = string.optional[EncryptionMetadata]("system", _.system)
  val credentials = Creds.schema.optional[EncryptionMetadata]("credentials", _.credentials)
  val partial = boolean.optional[EncryptionMetadata]("partial", _.partial)

  implicit val schema: Schema[EncryptionMetadata] = struct(
    system,
    credentials,
    partial,
  ){
    EncryptionMetadata.apply
  }.withId(ShapeId("smithy4s.benchmark", "EncryptionMetadata")).addHints(hints)
}
