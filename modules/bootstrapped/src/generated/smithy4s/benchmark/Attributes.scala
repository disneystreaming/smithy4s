package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Timestamp
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.timestamp

final case class Attributes(user: String, public: Boolean, size: Long, creationDate: Timestamp, region: String, queryable: Option[Boolean] = None, queryableLastChange: Option[Timestamp] = None, blockPublicAccess: Option[Boolean] = None, permissions: Option[List[Permission]] = None, tags: Option[List[String]] = None, backedUp: Option[Boolean] = None, metadata: Option[List[Metadata]] = None, encryption: Option[Encryption] = None)

object Attributes extends ShapeTag.Companion[Attributes] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Attributes")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Attributes] = struct(
    string.required[Attributes]("user", _.user),
    boolean.required[Attributes]("public", _.public),
    long.required[Attributes]("size", _.size),
    timestamp.required[Attributes]("creationDate", _.creationDate).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    string.required[Attributes]("region", _.region),
    boolean.optional[Attributes]("queryable", _.queryable),
    timestamp.optional[Attributes]("queryableLastChange", _.queryableLastChange).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    boolean.optional[Attributes]("blockPublicAccess", _.blockPublicAccess),
    ListPermissions.underlyingSchema.optional[Attributes]("permissions", _.permissions),
    ListTags.underlyingSchema.optional[Attributes]("tags", _.tags),
    boolean.optional[Attributes]("backedUp", _.backedUp),
    ListMetadata.underlyingSchema.optional[Attributes]("metadata", _.metadata),
    Encryption.schema.optional[Attributes]("encryption", _.encryption),
  ){
    Attributes.apply
  }.withId(id).addHints(hints)
}
