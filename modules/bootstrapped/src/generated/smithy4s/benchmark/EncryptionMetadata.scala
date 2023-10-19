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
  val id: ShapeId = ShapeId("smithy4s.benchmark", "EncryptionMetadata")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[EncryptionMetadata] = struct(
    string.optional[EncryptionMetadata]("system", _.system),
    Creds.schema.optional[EncryptionMetadata]("credentials", _.credentials),
    boolean.optional[EncryptionMetadata]("partial", _.partial),
  ){
    EncryptionMetadata.apply
  }.withId(id).addHints(hints)
}
