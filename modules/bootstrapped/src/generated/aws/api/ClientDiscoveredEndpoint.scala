package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** Indicates that the target operation should use the client's endpoint
  * discovery logic.
  * @param required
  *   This field denotes whether or not this operation requires the use of a
  *   specific endpoint. If this field is false, the standard regional
  *   endpoint for a service can handle this request. The client will start
  *   sending requests to the standard regional endpoint while working to
  *   discover a more specific endpoint.
  */
final case class ClientDiscoveredEndpoint(required: Boolean)
object ClientDiscoveredEndpoint extends ShapeTag.Companion[ClientDiscoveredEndpoint] {
  val id: ShapeId = ShapeId("aws.api", "clientDiscoveredEndpoint")

  val hints: Hints = Hints(
    smithy.api.Documentation("Indicates that the target operation should use the client\'s endpoint\ndiscovery logic."),
    smithy.api.Trait(selector = Some("operation"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[ClientDiscoveredEndpoint] = recursive(struct(
    boolean.required[ClientDiscoveredEndpoint]("required", _.required).addHints(smithy.api.Documentation("This field denotes whether or not this operation requires the use of a\nspecific endpoint. If this field is false, the standard regional\nendpoint for a service can handle this request. The client will start\nsending requests to the standard regional endpoint while working to\ndiscover a more specific endpoint."), smithy.api.Required()),
  ){
    ClientDiscoveredEndpoint.apply
  }.withId(id).addHints(hints))
}
