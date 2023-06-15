package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** An RPC-based protocol that sends 'POST' requests in the body as
  * `x-www-form-urlencoded` strings and responses in XML documents. This
  * protocol does not use HTTP binding traits.
  */
@deprecated(message = "N/A", since = "N/A")
final case class AwsQuery()
object AwsQuery extends ShapeTag.Companion[AwsQuery] {
  val id: ShapeId = ShapeId("aws.protocols", "awsQuery")

  val hints: Hints = Hints(
    smithy.api.ProtocolDefinition(traits = Some(List(smithy.api.TraitShapeId("aws.protocols#awsQueryError"), smithy.api.TraitShapeId("smithy.api#xmlAttribute"), smithy.api.TraitShapeId("smithy.api#xmlFlattened"), smithy.api.TraitShapeId("smithy.api#xmlName"), smithy.api.TraitShapeId("smithy.api#xmlNamespace"), smithy.api.TraitShapeId("smithy.api#timestampFormat"), smithy.api.TraitShapeId("smithy.api#cors"), smithy.api.TraitShapeId("smithy.api#endpoint"), smithy.api.TraitShapeId("smithy.api#hostLabel"))), noInlineDocumentSupport = Some(true)),
    smithy.api.Documentation("An RPC-based protocol that sends \'POST\' requests in the body as\n`x-www-form-urlencoded` strings and responses in XML documents. This\nprotocol does not use HTTP binding traits."),
    smithy.api.Trait(selector = Some("service [trait|xmlNamespace]"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
    smithy.api.Deprecated(message = None, since = None),
  )

  implicit val protocol: smithy4s.Protocol[AwsQuery] = new smithy4s.Protocol[AwsQuery] {
    def traits: Set[ShapeId] = Set(ShapeId("aws.protocols", "awsQueryError"), ShapeId("smithy.api", "xmlAttribute"), ShapeId("smithy.api", "xmlFlattened"), ShapeId("smithy.api", "xmlName"), ShapeId("smithy.api", "xmlNamespace"), ShapeId("smithy.api", "timestampFormat"), ShapeId("smithy.api", "cors"), ShapeId("smithy.api", "endpoint"), ShapeId("smithy.api", "hostLabel"))
  }

  implicit val schema: Schema[AwsQuery] = constant(AwsQuery()).withId(id).addHints(hints)
}
