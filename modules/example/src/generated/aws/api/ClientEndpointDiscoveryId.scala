package aws.api

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** Indicates members of the operation input which should be use to discover
  * endpoints.
  */
final case class ClientEndpointDiscoveryId()
object ClientEndpointDiscoveryId extends ShapeTag.Companion[ClientEndpointDiscoveryId] {
  val id: ShapeId = ShapeId("aws.api", "clientEndpointDiscoveryId")

  val hints: Hints = Hints(
    smithy.api.Documentation("Indicates members of the operation input which should be use to discover\nendpoints."),
    smithy.api.Trait(selector = Some("operation[trait|aws.api#clientDiscoveredEndpoint] -[input]->\nstructure > :test(member[trait|required] > string)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[ClientEndpointDiscoveryId] = constant(ClientEndpointDiscoveryId()).withId(id).addHints(hints)
}
