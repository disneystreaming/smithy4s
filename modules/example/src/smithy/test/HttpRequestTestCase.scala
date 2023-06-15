package smithy.test

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param requireQueryParams
  *   A list of query string parameter names that MUST appear in the
  *   serialized request URI, but no assertion is made on the value.
  *   
  *   Each value MUST appear in the format in which it is sent over the
  *   wire; if a key needs to be percent-encoded, then it MUST appear
  *   percent-encoded in this list.
  * @param requireHeaders
  *   A list of header field names that must appear in the serialized
  *   HTTP message, but no assertion is made on the value.
  *   
  *   Headers listed in `headers` do not need to appear in this list.
  * @param tags
  *   Applies a list of tags to the test.
  * @param authScheme
  *   The optional authentication scheme shape ID to assume. It's
  *   possible that specific authentication schemes might influence
  *   the serialization logic of an HTTP request.
  * @param vendorParams
  *   Defines vendor-specific parameters that are used to influence the
  *   request. For example, some vendors might utilize environment
  *   variables, configuration files on disk, or other means to influence
  *   the serialization formats used by clients or servers.
  *   
  *   If a `vendorParamsShape` is set, these parameters MUST be compatible
  *   with that shape's definition.
  * @param vendorParamsShape
  *   A shape to be used to validate the `vendorParams` member contents.
  *   
  *   If set, the parameters in `vendorParams` MUST be compatible with this
  *   shape's definition.
  * @param queryParams
  *   A list of the expected serialized query string parameters.
  *   
  *   Each element in the list is a query string key value pair
  *   that starts with the query string parameter name optionally
  *   followed by "=", optionally followed by the query string
  *   parameter value. For example, "foo=bar", "foo=", and "foo"
  *   are all valid values. The query string parameter name and
  *   the value MUST appear in the format in which it is expected
  *   to be sent over the wire; if a key or value needs to be
  *   percent-encoded, then it MUST appear percent-encoded in this list.
  *   
  *   A serialized HTTP request is not in compliance with the protocol
  *   if any query string parameter defined in `queryParams` is not
  *   defined in the request or if the value of a query string parameter
  *   in the request differs from the expected value.
  *   
  *   `queryParams` applies no constraints on additional query parameters.
  * @param appliesTo
  *   Indicates that the test case is only to be implemented by "client" or
  *   "server" implementations. This property is useful for identifying and
  *   testing edge cases of clients and servers that are impossible or
  *   undesirable to test in *both* client and server implementations.
  * @param forbidQueryParams
  *   A list of query string parameter names that must not appear in the
  *   serialized HTTP request.
  *   
  *   Each value MUST appear in the format in which it is sent over the
  *   wire; if a key needs to be percent-encoded, then it MUST appear
  *   percent-encoded in this list.
  * @param method
  *   The expected serialized HTTP request method.
  * @param body
  *   The expected HTTP message body.
  *   
  *   If no request body is defined, then no assertions are made about
  *   the body of the message.
  * @param documentation
  *   A description of the test and what is being asserted.
  * @param host
  *   The host / endpoint provided to the client, not including the path
  *   or scheme (for example, "example.com").
  * @param uri
  *   The request-target of the HTTP request, not including
  *   the query string (for example, "/foo/bar").
  * @param params
  *   Defines the input parameters used to generated the HTTP request.
  *   
  *   These parameters MUST be compatible with the input of the operation.
  * @param forbidHeaders
  *   A list of header field names that must not appear in the serialized
  *   HTTP request.
  * @param id
  *   The identifier of the test case. This identifier can be used by
  *   protocol test implementations to filter out unsupported test
  *   cases by ID, to generate test case names, etc. The provided `id`
  *   MUST match Smithy's `identifier` ABNF. No two `httpRequestTests`
  *   test cases can share the same ID.
  * @param resolvedHost
  *   The host / endpoint that the client should send to, not including
  *   the path or scheme (for example, "prefix.example.com").
  *   
  *   This can differ from the host provided to the client if the `hostPrefix`
  *   member of the `endpoint` trait is set, for instance.
  * @param headers
  *   Defines a map of expected HTTP headers.
  *   
  *   Headers that are not listed in this map are ignored unless they are
  *   explicitly forbidden through `forbidHeaders`.
  * @param protocol
  *   The name of the protocol to test.
  * @param bodyMediaType
  *   The media type of the `body`.
  *   
  *   This is used to help test runners to parse and validate the expected
  *   data against generated data.
  */
final case class HttpRequestTestCase(id: String, protocol: String, method: String, uri: String, host: Option[String] = None, resolvedHost: Option[String] = None, authScheme: Option[String] = None, queryParams: Option[List[String]] = None, forbidQueryParams: Option[List[String]] = None, requireQueryParams: Option[List[String]] = None, headers: Option[Map[String, String]] = None, forbidHeaders: Option[List[String]] = None, requireHeaders: Option[List[String]] = None, body: Option[String] = None, bodyMediaType: Option[String] = None, params: Option[Document] = None, vendorParams: Option[Document] = None, vendorParamsShape: Option[String] = None, documentation: Option[String] = None, tags: Option[List[NonEmptyString]] = None, appliesTo: Option[AppliesTo] = None)
object HttpRequestTestCase extends ShapeTag.Companion[HttpRequestTestCase] {
  val id: ShapeId = ShapeId("smithy.test", "HttpRequestTestCase")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  implicit val schema: Schema[HttpRequestTestCase] = struct(
    string.validated(smithy.api.Pattern("^[A-Za-z_][A-Za-z0-9_]+$")).required[HttpRequestTestCase]("id", _.id).addHints(smithy.api.Required(), smithy.api.Documentation("The identifier of the test case. This identifier can be used by\nprotocol test implementations to filter out unsupported test\ncases by ID, to generate test case names, etc. The provided `id`\nMUST match Smithy\'s `identifier` ABNF. No two `httpRequestTests`\ntest cases can share the same ID.")),
    string.required[HttpRequestTestCase]("protocol", _.protocol).addHints(smithy.api.IdRef(selector = "[trait|protocolDefinition]", failWhenMissing = Some(true), errorMessage = None), smithy.api.Required(), smithy.api.Documentation("The name of the protocol to test.")),
    string.validated(smithy.api.Length(min = Some(1L), max = None)).required[HttpRequestTestCase]("method", _.method).addHints(smithy.api.Required(), smithy.api.Documentation("The expected serialized HTTP request method.")),
    string.validated(smithy.api.Length(min = Some(1L), max = None)).required[HttpRequestTestCase]("uri", _.uri).addHints(smithy.api.Required(), smithy.api.Documentation("The request-target of the HTTP request, not including\nthe query string (for example, \"/foo/bar\").")),
    string.optional[HttpRequestTestCase]("host", _.host).addHints(smithy.api.Documentation("The host / endpoint provided to the client, not including the path\nor scheme (for example, \"example.com\").")),
    string.optional[HttpRequestTestCase]("resolvedHost", _.resolvedHost).addHints(smithy.api.Documentation("The host / endpoint that the client should send to, not including\nthe path or scheme (for example, \"prefix.example.com\").\n\nThis can differ from the host provided to the client if the `hostPrefix`\nmember of the `endpoint` trait is set, for instance.")),
    string.optional[HttpRequestTestCase]("authScheme", _.authScheme).addHints(smithy.api.Documentation("The optional authentication scheme shape ID to assume. It\'s\npossible that specific authentication schemes might influence\nthe serialization logic of an HTTP request."), smithy.api.IdRef(selector = "[trait|authDefinition]", failWhenMissing = Some(true), errorMessage = None)),
    StringList.underlyingSchema.optional[HttpRequestTestCase]("queryParams", _.queryParams).addHints(smithy.api.Documentation("A list of the expected serialized query string parameters.\n\nEach element in the list is a query string key value pair\nthat starts with the query string parameter name optionally\nfollowed by \"=\", optionally followed by the query string\nparameter value. For example, \"foo=bar\", \"foo=\", and \"foo\"\nare all valid values. The query string parameter name and\nthe value MUST appear in the format in which it is expected\nto be sent over the wire; if a key or value needs to be\npercent-encoded, then it MUST appear percent-encoded in this list.\n\nA serialized HTTP request is not in compliance with the protocol\nif any query string parameter defined in `queryParams` is not\ndefined in the request or if the value of a query string parameter\nin the request differs from the expected value.\n\n`queryParams` applies no constraints on additional query parameters.")),
    StringList.underlyingSchema.optional[HttpRequestTestCase]("forbidQueryParams", _.forbidQueryParams).addHints(smithy.api.Documentation("A list of query string parameter names that must not appear in the\nserialized HTTP request.\n\nEach value MUST appear in the format in which it is sent over the\nwire; if a key needs to be percent-encoded, then it MUST appear\npercent-encoded in this list.")),
    StringList.underlyingSchema.optional[HttpRequestTestCase]("requireQueryParams", _.requireQueryParams).addHints(smithy.api.Documentation("A list of query string parameter names that MUST appear in the\nserialized request URI, but no assertion is made on the value.\n\nEach value MUST appear in the format in which it is sent over the\nwire; if a key needs to be percent-encoded, then it MUST appear\npercent-encoded in this list.")),
    StringMap.underlyingSchema.optional[HttpRequestTestCase]("headers", _.headers).addHints(smithy.api.Documentation("Defines a map of expected HTTP headers.\n\nHeaders that are not listed in this map are ignored unless they are\nexplicitly forbidden through `forbidHeaders`.")),
    StringList.underlyingSchema.optional[HttpRequestTestCase]("forbidHeaders", _.forbidHeaders).addHints(smithy.api.Documentation("A list of header field names that must not appear in the serialized\nHTTP request.")),
    StringList.underlyingSchema.optional[HttpRequestTestCase]("requireHeaders", _.requireHeaders).addHints(smithy.api.Documentation("A list of header field names that must appear in the serialized\nHTTP message, but no assertion is made on the value.\n\nHeaders listed in `headers` do not need to appear in this list.")),
    string.optional[HttpRequestTestCase]("body", _.body).addHints(smithy.api.Documentation("The expected HTTP message body.\n\nIf no request body is defined, then no assertions are made about\nthe body of the message.")),
    string.optional[HttpRequestTestCase]("bodyMediaType", _.bodyMediaType).addHints(smithy.api.Documentation("The media type of the `body`.\n\nThis is used to help test runners to parse and validate the expected\ndata against generated data.")),
    document.optional[HttpRequestTestCase]("params", _.params).addHints(smithy.api.Documentation("Defines the input parameters used to generated the HTTP request.\n\nThese parameters MUST be compatible with the input of the operation.")),
    document.optional[HttpRequestTestCase]("vendorParams", _.vendorParams).addHints(smithy.api.Documentation("Defines vendor-specific parameters that are used to influence the\nrequest. For example, some vendors might utilize environment\nvariables, configuration files on disk, or other means to influence\nthe serialization formats used by clients or servers.\n\nIf a `vendorParamsShape` is set, these parameters MUST be compatible\nwith that shape\'s definition.")),
    string.optional[HttpRequestTestCase]("vendorParamsShape", _.vendorParamsShape).addHints(smithy.api.Documentation("A shape to be used to validate the `vendorParams` member contents.\n\nIf set, the parameters in `vendorParams` MUST be compatible with this\nshape\'s definition."), smithy.api.IdRef(selector = "*", failWhenMissing = Some(true), errorMessage = None)),
    string.optional[HttpRequestTestCase]("documentation", _.documentation).addHints(smithy.api.Documentation("A description of the test and what is being asserted.")),
    NonEmptyStringList.underlyingSchema.optional[HttpRequestTestCase]("tags", _.tags).addHints(smithy.api.Documentation("Applies a list of tags to the test.")),
    AppliesTo.schema.optional[HttpRequestTestCase]("appliesTo", _.appliesTo).addHints(smithy.api.Documentation("Indicates that the test case is only to be implemented by \"client\" or\n\"server\" implementations. This property is useful for identifying and\ntesting edge cases of clients and servers that are impossible or\nundesirable to test in *both* client and server implementations.")),
  ){
    HttpRequestTestCase.apply
  }.withId(id).addHints(hints)
}
