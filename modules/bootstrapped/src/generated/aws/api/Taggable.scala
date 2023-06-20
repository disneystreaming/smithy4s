package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Indicates a resource supports CRUD operations for tags. Either through
  * resource lifecycle or instance operations or tagging operations on the
  * service.
  * @param property
  *   The `property` property is a string value that identifies which
  *   resource property represents tags for the resource.
  * @param apiConfig
  *   Specifies configuration for resource specific tagging APIs if the
  *   resource has them.
  * @param disableSystemTags
  *   Flag indicating if the resource is not able to carry AWS system level.
  *   Used by service principals. Default value is `false`
  */
final case class Taggable(property: Option[String] = None, apiConfig: Option[TaggableApiConfig] = None, disableSystemTags: Option[Boolean] = None)
object Taggable extends ShapeTag.Companion[Taggable] {
  val id: ShapeId = ShapeId("aws.api", "taggable")

  val hints: Hints = Hints(
    smithy.api.Documentation("Indicates a resource supports CRUD operations for tags. Either through\nresource lifecycle or instance operations or tagging operations on the\nservice."),
    smithy.api.Unstable(),
    smithy.api.Trait(selector = Some("resource"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Taggable] = recursive(struct(
    string.optional[Taggable]("property", _.property).addHints(smithy.api.Documentation("The `property` property is a string value that identifies which\nresource property represents tags for the resource.")),
    TaggableApiConfig.schema.optional[Taggable]("apiConfig", _.apiConfig).addHints(smithy.api.Documentation("Specifies configuration for resource specific tagging APIs if the\nresource has them.")),
    boolean.optional[Taggable]("disableSystemTags", _.disableSystemTags).addHints(smithy.api.Documentation("Flag indicating if the resource is not able to carry AWS system level.\nUsed by service principals. Default value is `false`")),
  ){
    Taggable.apply
  }.withId(id).addHints(hints))
}
