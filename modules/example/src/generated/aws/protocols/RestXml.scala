package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** A RESTful protocol that sends XML in structured payloads.
  * @param http
  *   The priority ordered list of supported HTTP protocol versions.
  * @param eventStreamHttp
  *   The priority ordered list of supported HTTP protocol versions that
  *   are required when using event streams with the service. If not set,
  *   this value defaults to the value of the `http` member. Any entry in
  *   `eventStreamHttp` MUST also appear in `http`.
  * @param noErrorWrapping
  *   Disables the serialization wrapping of error properties in an 'Error'
  *   XML element.
  */
@deprecated(message = "N/A", since = "N/A")
final case class RestXml(http: Option[List[String]] = None, eventStreamHttp: Option[List[String]] = None, @deprecated(message = "N/A", since = "N/A") noErrorWrapping: Option[Boolean] = None) extends HttpConfiguration
object RestXml extends ShapeTag.Companion[RestXml] {
  val id: ShapeId = ShapeId("aws.protocols", "restXml")

  val hints: Hints = Hints(
    smithy.api.ProtocolDefinition(traits = Some(List(smithy.api.TraitShapeId("smithy.api#cors"), smithy.api.TraitShapeId("smithy.api#endpoint"), smithy.api.TraitShapeId("smithy.api#hostLabel"), smithy.api.TraitShapeId("smithy.api#http"), smithy.api.TraitShapeId("smithy.api#httpError"), smithy.api.TraitShapeId("smithy.api#httpHeader"), smithy.api.TraitShapeId("smithy.api#httpLabel"), smithy.api.TraitShapeId("smithy.api#httpPayload"), smithy.api.TraitShapeId("smithy.api#httpPrefixHeaders"), smithy.api.TraitShapeId("smithy.api#httpQuery"), smithy.api.TraitShapeId("smithy.api#httpQueryParams"), smithy.api.TraitShapeId("smithy.api#httpResponseCode"), smithy.api.TraitShapeId("smithy.api#xmlAttribute"), smithy.api.TraitShapeId("smithy.api#xmlFlattened"), smithy.api.TraitShapeId("smithy.api#xmlName"), smithy.api.TraitShapeId("smithy.api#xmlNamespace"))), noInlineDocumentSupport = Some(true)),
    smithy.api.Documentation("A RESTful protocol that sends XML in structured payloads."),
    smithy.api.Trait(selector = Some("service"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
    smithy.api.Deprecated(message = None, since = None),
  )

  implicit val protocol: smithy4s.Protocol[RestXml] = new smithy4s.Protocol[RestXml] {
    def traits: Set[ShapeId] = Set(ShapeId("smithy.api", "cors"), ShapeId("smithy.api", "endpoint"), ShapeId("smithy.api", "hostLabel"), ShapeId("smithy.api", "http"), ShapeId("smithy.api", "httpError"), ShapeId("smithy.api", "httpHeader"), ShapeId("smithy.api", "httpLabel"), ShapeId("smithy.api", "httpPayload"), ShapeId("smithy.api", "httpPrefixHeaders"), ShapeId("smithy.api", "httpQuery"), ShapeId("smithy.api", "httpQueryParams"), ShapeId("smithy.api", "httpResponseCode"), ShapeId("smithy.api", "xmlAttribute"), ShapeId("smithy.api", "xmlFlattened"), ShapeId("smithy.api", "xmlName"), ShapeId("smithy.api", "xmlNamespace"))
  }

  implicit val schema: Schema[RestXml] = recursive(struct(
    StringList.underlyingSchema.optional[RestXml]("http", _.http).addHints(smithy.api.Documentation("The priority ordered list of supported HTTP protocol versions.")),
    StringList.underlyingSchema.optional[RestXml]("eventStreamHttp", _.eventStreamHttp).addHints(smithy.api.Documentation("The priority ordered list of supported HTTP protocol versions that\nare required when using event streams with the service. If not set,\nthis value defaults to the value of the `http` member. Any entry in\n`eventStreamHttp` MUST also appear in `http`.")),
    boolean.optional[RestXml]("noErrorWrapping", _.noErrorWrapping).addHints(smithy.api.Documentation("Disables the serialization wrapping of error properties in an \'Error\'\nXML element."), smithy.api.Deprecated(message = None, since = None)),
  ){
    RestXml.apply
  }.withId(id).addHints(hints))
}
