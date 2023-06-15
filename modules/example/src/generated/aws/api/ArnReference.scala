package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Marks a string as containing an ARN.
  * @param type
  *   The AWS CloudFormation resource type contained in the ARN.
  * @param resource
  *   An absolute shape ID that references the Smithy resource type contained
  *   in the ARN (e.g., `com.foo#SomeResource`). The targeted resource is not
  *   required to be found in the model, allowing for external shapes to be
  *   referenced without needing to take on an additional dependency. If the
  *   shape is found in the model, it MUST target a resource shape, and the
  *   resource MUST be found within the closure of the referenced service
  *   shape.
  * @param service
  *   The Smithy service absolute shape ID that is referenced by the ARN. The
  *   targeted service is not required to be found in the model, allowing for
  *   external shapes to be referenced without needing to take on an
  *   additional dependency.
  */
final case class ArnReference(_type: Option[String] = None, resource: Option[String] = None, service: Option[String] = None)
object ArnReference extends ShapeTag.Companion[ArnReference] {
  val id: ShapeId = ShapeId("aws.api", "arnReference")

  val hints: Hints = Hints(
    smithy.api.Documentation("Marks a string as containing an ARN."),
    smithy.api.Trait(selector = Some("string"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[ArnReference] = recursive(struct(
    string.optional[ArnReference]("type", _._type).addHints(smithy.api.Documentation("The AWS CloudFormation resource type contained in the ARN.")),
    string.optional[ArnReference]("resource", _.resource).addHints(smithy.api.Documentation("An absolute shape ID that references the Smithy resource type contained\nin the ARN (e.g., `com.foo#SomeResource`). The targeted resource is not\nrequired to be found in the model, allowing for external shapes to be\nreferenced without needing to take on an additional dependency. If the\nshape is found in the model, it MUST target a resource shape, and the\nresource MUST be found within the closure of the referenced service\nshape.")),
    string.optional[ArnReference]("service", _.service).addHints(smithy.api.Documentation("The Smithy service absolute shape ID that is referenced by the ARN. The\ntargeted service is not required to be found in the model, allowing for\nexternal shapes to be referenced without needing to take on an\nadditional dependency.")),
  ){
    ArnReference.apply
  }.withId(id).addHints(hints))
}
