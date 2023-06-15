package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** A RESTful protocol that sends JSON in structured payloads.
  * @param http
  *   The priority ordered list of supported HTTP protocol versions.
  * @param eventStreamHttp
  *   The priority ordered list of supported HTTP protocol versions that
  *   are required when using event streams with the service. If not set,
  *   this value defaults to the value of the `http` member. Any entry in
  *   `eventStreamHttp` MUST also appear in `http`.
  */
final case class RestJson1(http: Option[List[String]] = None, eventStreamHttp: Option[List[String]] = None) extends HttpConfiguration
object RestJson1 extends ShapeTag.Companion[RestJson1] {
  val id: ShapeId = ShapeId("aws.protocols", "restJson1")

  val hints: Hints = Hints(
    smithy.api.Documentation("A RESTful protocol that sends JSON in structured payloads."),
    smithy.api.ProtocolDefinition(traits = Some(List(smithy.api.TraitShapeId("smithy.api#cors"), smithy.api.TraitShapeId("smithy.api#endpoint"), smithy.api.TraitShapeId("smithy.api#hostLabel"), smithy.api.TraitShapeId("smithy.api#http"), smithy.api.TraitShapeId("smithy.api#httpError"), smithy.api.TraitShapeId("smithy.api#httpHeader"), smithy.api.TraitShapeId("smithy.api#httpLabel"), smithy.api.TraitShapeId("smithy.api#httpPayload"), smithy.api.TraitShapeId("smithy.api#httpPrefixHeaders"), smithy.api.TraitShapeId("smithy.api#httpQuery"), smithy.api.TraitShapeId("smithy.api#httpQueryParams"), smithy.api.TraitShapeId("smithy.api#httpResponseCode"), smithy.api.TraitShapeId("smithy.api#jsonName"), smithy.api.TraitShapeId("smithy.api#timestampFormat"))), noInlineDocumentSupport = None),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val protocol: smithy4s.Protocol[RestJson1] = new smithy4s.Protocol[RestJson1] {
    def traits: Set[ShapeId] = Set(ShapeId("smithy.api", "cors"), ShapeId("smithy.api", "endpoint"), ShapeId("smithy.api", "hostLabel"), ShapeId("smithy.api", "http"), ShapeId("smithy.api", "httpError"), ShapeId("smithy.api", "httpHeader"), ShapeId("smithy.api", "httpLabel"), ShapeId("smithy.api", "httpPayload"), ShapeId("smithy.api", "httpPrefixHeaders"), ShapeId("smithy.api", "httpQuery"), ShapeId("smithy.api", "httpQueryParams"), ShapeId("smithy.api", "httpResponseCode"), ShapeId("smithy.api", "jsonName"), ShapeId("smithy.api", "timestampFormat"))
  }

  implicit val schema: Schema[RestJson1] = recursive(struct(
    StringList.underlyingSchema.optional[RestJson1]("http", _.http).addHints(smithy.api.Documentation("The priority ordered list of supported HTTP protocol versions.")),
    StringList.underlyingSchema.optional[RestJson1]("eventStreamHttp", _.eventStreamHttp).addHints(smithy.api.Documentation("The priority ordered list of supported HTTP protocol versions that\nare required when using event streams with the service. If not set,\nthis value defaults to the value of the `http` member. Any entry in\n`eventStreamHttp` MUST also appear in `http`.")),
  ){
    RestJson1.apply
  }.withId(id).addHints(hints))
}
