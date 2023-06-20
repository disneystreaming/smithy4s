package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Configures endpoint discovery for the service.
  * @param operation
  *   Indicates the operation that clients should use to discover endpoints
  *   for the service.
  * @param error
  *   Indicates the error that tells clients that the endpoint they are using
  *   is no longer valid. This error MUST be bound to any operation bound to
  *   the service which is marked with the aws.api#clientDiscoveredEndpoint
  *   trait.
  */
final case class ClientEndpointDiscovery(operation: String, error: Option[String] = None)
object ClientEndpointDiscovery extends ShapeTag.Companion[ClientEndpointDiscovery] {
  val id: ShapeId = ShapeId("aws.api", "clientEndpointDiscovery")

  val hints: Hints = Hints(
    smithy.api.Documentation("Configures endpoint discovery for the service."),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[ClientEndpointDiscovery] = recursive(struct(
    string.required[ClientEndpointDiscovery]("operation", _.operation).addHints(smithy.api.IdRef(selector = "operation", failWhenMissing = Some(true), errorMessage = None), smithy.api.Required(), smithy.api.Documentation("Indicates the operation that clients should use to discover endpoints\nfor the service.")),
    string.optional[ClientEndpointDiscovery]("error", _.error).addHints(smithy.api.IdRef(selector = "structure[trait|error]", failWhenMissing = Some(true), errorMessage = None), smithy.api.Documentation("Indicates the error that tells clients that the endpoint they are using\nis no longer valid. This error MUST be bound to any operation bound to\nthe service which is marked with the aws.api#clientDiscoveredEndpoint\ntrait."), smithy.api.Recommended(reason = None)),
  ){
    ClientEndpointDiscovery.apply
  }.withId(id).addHints(hints))
}
