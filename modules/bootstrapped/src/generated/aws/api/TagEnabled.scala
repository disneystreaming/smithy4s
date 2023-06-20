package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** Annotates a service as having tagging on 1 or more resources and associated
  * APIs to perform CRUD operations on those tags
  * @param disableDefaultOperations
  *   The `disableDefaultOperations` property is a boolean value that specifies
  *   if the service does not have the standard tag operations supporting all
  *   resources on the service. Default value is `false`
  */
final case class TagEnabled(disableDefaultOperations: Option[Boolean] = None)
object TagEnabled extends ShapeTag.Companion[TagEnabled] {
  val id: ShapeId = ShapeId("aws.api", "tagEnabled")

  val hints: Hints = Hints(
    smithy.api.Documentation("Annotates a service as having tagging on 1 or more resources and associated\nAPIs to perform CRUD operations on those tags"),
    smithy.api.Unstable(),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[TagEnabled] = recursive(struct(
    boolean.optional[TagEnabled]("disableDefaultOperations", _.disableDefaultOperations).addHints(smithy.api.Documentation("The `disableDefaultOperations` property is a boolean value that specifies\nif the service does not have the standard tag operations supporting all\nresources on the service. Default value is `false`")),
  ){
    TagEnabled.apply
  }.withId(id).addHints(hints))
}
