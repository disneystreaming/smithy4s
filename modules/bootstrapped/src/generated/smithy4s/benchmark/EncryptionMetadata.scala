package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.string

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
