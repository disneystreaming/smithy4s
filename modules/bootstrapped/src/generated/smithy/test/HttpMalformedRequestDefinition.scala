package smithy.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param host
  *   The host / endpoint provided to the client, not including the path
  *   or scheme (for example, "example.com").
  * @param uri
  *   The request-target of the HTTP request, not including
  *   the query string (for example, "/foo/bar").
  * @param queryParams
  *   A list of the serialized query string parameters to include in the request.
  *   
  *   Each element in the list is a query string key value pair
  *   that starts with the query string parameter name optionally
  *   followed by "=", optionally followed by the query string
  *   parameter value. For example, "foo=bar", "foo=", and "foo"
  *   are all valid values. The query string parameter name and
  *   the value MUST appear in the format in which it is expected
  *   to be sent over the wire; if a key or value needs to be
  *   percent-encoded, then it MUST appear percent-encoded in this list.
  * @param headers
  *   Defines a map of HTTP headers to include in the request
  * @param method
  *   The HTTP request method.
  * @param body
  *   The HTTP message body to include in the request
  */
final case class HttpMalformedRequestDefinition(method: String, uri: String, host: Option[String] = None, queryParams: Option[List[String]] = None, headers: Option[Map[String, String]] = None, body: Option[String] = None)
object HttpMalformedRequestDefinition extends ShapeTag.Companion[HttpMalformedRequestDefinition] {
  val id: ShapeId = ShapeId("smithy.test", "HttpMalformedRequestDefinition")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  implicit val schema: Schema[HttpMalformedRequestDefinition] = struct(
    string.validated(smithy.api.Length(min = Some(1L), max = None)).required[HttpMalformedRequestDefinition]("method", _.method).addHints(smithy.api.Required(), smithy.api.Documentation("The HTTP request method.")),
    string.validated(smithy.api.Length(min = Some(1L), max = None)).required[HttpMalformedRequestDefinition]("uri", _.uri).addHints(smithy.api.Required(), smithy.api.Documentation("The request-target of the HTTP request, not including\nthe query string (for example, \"/foo/bar\").")),
    string.optional[HttpMalformedRequestDefinition]("host", _.host).addHints(smithy.api.Documentation("The host / endpoint provided to the client, not including the path\nor scheme (for example, \"example.com\").")),
    StringList.underlyingSchema.optional[HttpMalformedRequestDefinition]("queryParams", _.queryParams).addHints(smithy.api.Documentation("A list of the serialized query string parameters to include in the request.\n\nEach element in the list is a query string key value pair\nthat starts with the query string parameter name optionally\nfollowed by \"=\", optionally followed by the query string\nparameter value. For example, \"foo=bar\", \"foo=\", and \"foo\"\nare all valid values. The query string parameter name and\nthe value MUST appear in the format in which it is expected\nto be sent over the wire; if a key or value needs to be\npercent-encoded, then it MUST appear percent-encoded in this list.")),
    StringMap.underlyingSchema.optional[HttpMalformedRequestDefinition]("headers", _.headers).addHints(smithy.api.Documentation("Defines a map of HTTP headers to include in the request")),
    string.optional[HttpMalformedRequestDefinition]("body", _.body).addHints(smithy.api.Documentation("The HTTP message body to include in the request")),
  ){
    HttpMalformedRequestDefinition.apply
  }.withId(id).addHints(hints)
}
