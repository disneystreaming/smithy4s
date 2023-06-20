package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** Structure representing the configuration of resource specific tagging APIs
  * @param tagApi
  *   The `tagApi` property is a string value that references a non-instance
  *   or create operation that creates or updates tags on the resource.
  * @param untagApi
  *   The `untagApi` property is a string value that references a non-instance
  *   operation that removes tags on the resource.
  * @param listTagsApi
  *   The `listTagsApi` property is a string value that references a non-
  *   instance operation which gets the current tags on the resource.
  */
final case class TaggableApiConfig(tagApi: TagOperationReference, untagApi: TagOperationReference, listTagsApi: TagOperationReference)
object TaggableApiConfig extends ShapeTag.Companion[TaggableApiConfig] {
  val id: ShapeId = ShapeId("aws.api", "TaggableApiConfig")

  val hints: Hints = Hints(
    smithy.api.Documentation("Structure representing the configuration of resource specific tagging APIs"),
  )

  implicit val schema: Schema[TaggableApiConfig] = struct(
    TagOperationReference.schema.required[TaggableApiConfig]("tagApi", _.tagApi).addHints(smithy.api.Documentation("The `tagApi` property is a string value that references a non-instance\nor create operation that creates or updates tags on the resource."), smithy.api.Required()),
    TagOperationReference.schema.required[TaggableApiConfig]("untagApi", _.untagApi).addHints(smithy.api.Documentation("The `untagApi` property is a string value that references a non-instance\noperation that removes tags on the resource."), smithy.api.Required()),
    TagOperationReference.schema.required[TaggableApiConfig]("listTagsApi", _.listTagsApi).addHints(smithy.api.Documentation("The `listTagsApi` property is a string value that references a non-\ninstance operation which gets the current tags on the resource."), smithy.api.Required()),
  ){
    TaggableApiConfig.apply
  }.withId(id).addHints(hints)
}
