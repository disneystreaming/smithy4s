package smithy4s.benchmark

import smithy.api.Required
import smithy.api.TimestampFormat
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class Attributes(user: String, public: Boolean, size: Long, creationDate: Timestamp, region: String, queryable: Option[Boolean] = None, queryableLastChange: Option[Timestamp] = None, blockPublicAccess: Option[Boolean] = None, permissions: Option[List[Permission]] = None, tags: Option[List[String]] = None, backedUp: Option[Boolean] = None, metadata: Option[List[Metadata]] = None, encryption: Option[Encryption] = None)
object Attributes extends ShapeTag.$Companion[Attributes] {
  val $id: ShapeId = ShapeId("smithy4s.benchmark", "Attributes")

  val $hints: Hints = Hints.empty

  val user: FieldLens[Attributes, String] = string.required[Attributes]("user", _.user, n => c => c.copy(user = n)).addHints(Required())
  val public: FieldLens[Attributes, Boolean] = boolean.required[Attributes]("public", _.public, n => c => c.copy(public = n)).addHints(Required())
  val size: FieldLens[Attributes, Long] = long.required[Attributes]("size", _.size, n => c => c.copy(size = n)).addHints(Required())
  val creationDate: FieldLens[Attributes, Timestamp] = timestamp.required[Attributes]("creationDate", _.creationDate, n => c => c.copy(creationDate = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen, Required())
  val region: FieldLens[Attributes, String] = string.required[Attributes]("region", _.region, n => c => c.copy(region = n)).addHints(Required())
  val queryable: FieldLens[Attributes, Option[Boolean]] = boolean.optional[Attributes]("queryable", _.queryable, n => c => c.copy(queryable = n))
  val queryableLastChange: FieldLens[Attributes, Option[Timestamp]] = timestamp.optional[Attributes]("queryableLastChange", _.queryableLastChange, n => c => c.copy(queryableLastChange = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen)
  val blockPublicAccess: FieldLens[Attributes, Option[Boolean]] = boolean.optional[Attributes]("blockPublicAccess", _.blockPublicAccess, n => c => c.copy(blockPublicAccess = n))
  val permissions: FieldLens[Attributes, Option[List[Permission]]] = ListPermissions.underlyingSchema.optional[Attributes]("permissions", _.permissions, n => c => c.copy(permissions = n))
  val tags: FieldLens[Attributes, Option[List[String]]] = ListTags.underlyingSchema.optional[Attributes]("tags", _.tags, n => c => c.copy(tags = n))
  val backedUp: FieldLens[Attributes, Option[Boolean]] = boolean.optional[Attributes]("backedUp", _.backedUp, n => c => c.copy(backedUp = n))
  val metadata: FieldLens[Attributes, Option[List[Metadata]]] = ListMetadata.underlyingSchema.optional[Attributes]("metadata", _.metadata, n => c => c.copy(metadata = n))
  val encryption: FieldLens[Attributes, Option[Encryption]] = Encryption.$schema.optional[Attributes]("encryption", _.encryption, n => c => c.copy(encryption = n))

  implicit val $schema: Schema[Attributes] = struct(
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
  }.withId($id).addHints($hints)
}
