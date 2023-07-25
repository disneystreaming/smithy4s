package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Attributes(user: String, public: Boolean, size: Long, creationDate: Timestamp, region: String, queryable: Option[Boolean] = None, queryableLastChange: Option[Timestamp] = None, blockPublicAccess: Option[Boolean] = None, permissions: Option[List[Permission]] = None, tags: Option[List[String]] = None, backedUp: Option[Boolean] = None, metadata: Option[List[Metadata]] = None, encryption: Option[Encryption] = None)
object Attributes extends ShapeTag.Companion[Attributes] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Attributes")

  val hints: Hints = Hints.empty

  val user = string.required[Attributes]("user", _.user).addHints(smithy.api.Required())
  val public = boolean.required[Attributes]("public", _.public).addHints(smithy.api.Required())
  val size = long.required[Attributes]("size", _.size).addHints(smithy.api.Required())
  val creationDate = timestamp.required[Attributes]("creationDate", _.creationDate).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.Required())
  val region = string.required[Attributes]("region", _.region).addHints(smithy.api.Required())
  val queryable = boolean.optional[Attributes]("queryable", _.queryable)
  val queryableLastChange = timestamp.optional[Attributes]("queryableLastChange", _.queryableLastChange).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen)
  val blockPublicAccess = boolean.optional[Attributes]("blockPublicAccess", _.blockPublicAccess)
  val permissions = ListPermissions.underlyingSchema.optional[Attributes]("permissions", _.permissions)
  val tags = ListTags.underlyingSchema.optional[Attributes]("tags", _.tags)
  val backedUp = boolean.optional[Attributes]("backedUp", _.backedUp)
  val metadata = ListMetadata.underlyingSchema.optional[Attributes]("metadata", _.metadata)
  val encryption = Encryption.schema.optional[Attributes]("encryption", _.encryption)

  implicit val schema: Schema[Attributes] = struct(
    user,
    public,
    size,
    creationDate,
    region,
    queryable,
    queryableLastChange,
    blockPublicAccess,
    permissions,
    tags,
    backedUp,
    metadata,
    encryption,
  ){
    Attributes.apply
  }.withId(id).addHints(hints)
}
