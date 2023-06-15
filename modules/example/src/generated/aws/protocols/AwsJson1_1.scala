package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** An RPC-based protocol that sends JSON payloads. This protocol does not use
  * HTTP binding traits.
  * @param http
  *   The priority ordered list of supported HTTP protocol versions.
  * @param eventStreamHttp
  *   The priority ordered list of supported HTTP protocol versions that
  *   are required when using event streams with the service. If not set,
  *   this value defaults to the value of the `http` member. Any entry in
  *   `eventStreamHttp` MUST also appear in `http`.
  */
final case class AwsJson1_1(http: Option[List[String]] = None, eventStreamHttp: Option[List[String]] = None) extends HttpConfiguration
object AwsJson1_1 extends ShapeTag.Companion[AwsJson1_1] {
  val id: ShapeId = ShapeId("aws.protocols", "awsJson1_1")

  val hints: Hints = Hints(
    smithy.api.Documentation("An RPC-based protocol that sends JSON payloads. This protocol does not use\nHTTP binding traits."),
    smithy.api.ProtocolDefinition(traits = Some(List(smithy.api.TraitShapeId("smithy.api#timestampFormat"), smithy.api.TraitShapeId("smithy.api#cors"), smithy.api.TraitShapeId("smithy.api#endpoint"), smithy.api.TraitShapeId("smithy.api#hostLabel"))), noInlineDocumentSupport = None),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val protocol: smithy4s.Protocol[AwsJson1_1] = new smithy4s.Protocol[AwsJson1_1] {
    def traits: Set[ShapeId] = Set(ShapeId("smithy.api", "timestampFormat"), ShapeId("smithy.api", "cors"), ShapeId("smithy.api", "endpoint"), ShapeId("smithy.api", "hostLabel"))
  }

  implicit val schema: Schema[AwsJson1_1] = recursive(struct(
    StringList.underlyingSchema.optional[AwsJson1_1]("http", _.http).addHints(smithy.api.Documentation("The priority ordered list of supported HTTP protocol versions.")),
    StringList.underlyingSchema.optional[AwsJson1_1]("eventStreamHttp", _.eventStreamHttp).addHints(smithy.api.Documentation("The priority ordered list of supported HTTP protocol versions that\nare required when using event streams with the service. If not set,\nthis value defaults to the value of the `http` member. Any entry in\n`eventStreamHttp` MUST also appear in `http`.")),
  ){
    AwsJson1_1.apply
  }.withId(id).addHints(hints))
}
