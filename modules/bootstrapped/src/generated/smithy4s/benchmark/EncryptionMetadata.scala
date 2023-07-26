package smithy4s.benchmark

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EncryptionMetadata(system: Option[String] = None, credentials: Option[Creds] = None, partial: Option[Boolean] = None)
object EncryptionMetadata extends ShapeTag.Companion[EncryptionMetadata] {

  val system: FieldLens[EncryptionMetadata, Option[String]] = string.optional[EncryptionMetadata]("system", _.system, n => c => c.copy(system = n))
  val credentials: FieldLens[EncryptionMetadata, Option[Creds]] = Creds.schema.optional[EncryptionMetadata]("credentials", _.credentials, n => c => c.copy(credentials = n))
  val partial: FieldLens[EncryptionMetadata, Option[Boolean]] = boolean.optional[EncryptionMetadata]("partial", _.partial, n => c => c.copy(partial = n))

  implicit val schema: Schema[EncryptionMetadata] = struct(
    system,
    credentials,
    partial,
  ){
    EncryptionMetadata.apply
  }
  .withId(ShapeId("smithy4s.benchmark", "EncryptionMetadata"))
}
