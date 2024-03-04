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

  // constructor using the original order from the spec
  private def make(user: String, public: Boolean, size: Long, creationDate: Timestamp, region: String, queryable: Option[Boolean], queryableLastChange: Option[Timestamp], blockPublicAccess: Option[Boolean], permissions: Option[List[Permission]], tags: Option[List[String]], backedUp: Option[Boolean], metadata: Option[List[Metadata]], encryption: Option[Encryption]): Attributes = Attributes(user, public, size, creationDate, region, queryable, queryableLastChange, blockPublicAccess, permissions, tags, backedUp, metadata, encryption)

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
  )(make).withId(id).addHints(hints)
}
