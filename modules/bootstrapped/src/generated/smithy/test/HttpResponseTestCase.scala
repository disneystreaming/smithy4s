package smithy.test

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param body
  *   Defines the HTTP message body.
  *   
  *   If no response body is defined, then no assertions are made about
  *   the body of the message.
  * @param requireHeaders
  *   A list of header field names that must appear in the serialized
  *   HTTP message, but no assertion is made on the value.
  *   
  *   Headers listed in `headers` map do not need to appear in this list.
  * @param tags
  *   Applies a list of tags to the test.
  * @param documentation
  *   A description of the test and what is being asserted.
  * @param params
  *   Defines the output parameters deserialized from the HTTP response.
  *   
  *   These parameters MUST be compatible with the output of the operation.
  * @param authScheme
  *   The optional authentication scheme shape ID to assume. It's possible
  *   that specific authentication schemes might influence the serialization
  *   logic of an HTTP response.
  * @param code
  *   Defines the HTTP response code.
  * @param vendorParams
  *   Defines vendor-specific parameters that are used to influence the
  *   response. For example, some vendors might utilize environment
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
  * @param headers
  *   A map of expected HTTP headers. Each key represents a header field
  *   name and each value represents the expected header value. An HTTP
  *   response is not in compliance with the protocol if any listed header
  *   is missing from the serialized response or if the expected header
  *   value differs from the serialized response value.
  *   
  *   `headers` applies no constraints on additional headers.
  * @param appliesTo
  *   Indicates that the test case is only to be implemented by "client" or
  *   "server" implementations. This property is useful for identifying and
  *   testing edge cases of clients and servers that are impossible or
  *   undesirable to test in *both* client and server implementations.
  * @param forbidHeaders
  *   A list of header field names that must not appear.
  * @param id
  *   The identifier of the test case. This identifier can be used by
  *   protocol test implementations to filter out unsupported test
  *   cases by ID, to generate test case names, etc. The provided `id`
  *   MUST match Smithy's `identifier` ABNF. No two `httpResponseTests`
  *   test cases can share the same ID.
  * @param protocol
  *   The shape ID of the protocol to test.
  * @param bodyMediaType
  *   The media type of the `body`.
  *   
  *   This is used to help test runners to parse and validate the expected
  *   data against generated data. Binary media type formats require that
  *   the contents of `body` are base64 encoded.
  */
final case class HttpResponseTestCase(id: String, protocol: String, code: Int, authScheme: Option[String] = None, headers: Option[Map[String, String]] = None, forbidHeaders: Option[List[String]] = None, requireHeaders: Option[List[String]] = None, body: Option[String] = None, bodyMediaType: Option[String] = None, params: Option[Document] = None, vendorParams: Option[Document] = None, vendorParamsShape: Option[String] = None, documentation: Option[String] = None, tags: Option[List[NonEmptyString]] = None, appliesTo: Option[AppliesTo] = None)
object HttpResponseTestCase extends ShapeTag.Companion[HttpResponseTestCase] {
  val id: ShapeId = ShapeId("smithy.test", "HttpResponseTestCase")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  implicit val schema: Schema[HttpResponseTestCase] = struct(
    string.validated(smithy.api.Pattern("^[A-Za-z_][A-Za-z0-9_]+$")).required[HttpResponseTestCase]("id", _.id).addHints(smithy.api.Required(), smithy.api.Documentation("The identifier of the test case. This identifier can be used by\nprotocol test implementations to filter out unsupported test\ncases by ID, to generate test case names, etc. The provided `id`\nMUST match Smithy\'s `identifier` ABNF. No two `httpResponseTests`\ntest cases can share the same ID.")),
    string.required[HttpResponseTestCase]("protocol", _.protocol).addHints(smithy.api.IdRef(selector = "[trait|protocolDefinition]", failWhenMissing = Some(true), errorMessage = None), smithy.api.Required(), smithy.api.Documentation("The shape ID of the protocol to test.")),
    int.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(100.0)), max = Some(scala.math.BigDecimal(599.0)))).required[HttpResponseTestCase]("code", _.code).addHints(smithy.api.Required(), smithy.api.Documentation("Defines the HTTP response code.")),
    string.optional[HttpResponseTestCase]("authScheme", _.authScheme).addHints(smithy.api.Documentation("The optional authentication scheme shape ID to assume. It\'s possible\nthat specific authentication schemes might influence the serialization\nlogic of an HTTP response."), smithy.api.IdRef(selector = "[trait|authDefinition]", failWhenMissing = Some(true), errorMessage = None)),
    StringMap.underlyingSchema.optional[HttpResponseTestCase]("headers", _.headers).addHints(smithy.api.Documentation("A map of expected HTTP headers. Each key represents a header field\nname and each value represents the expected header value. An HTTP\nresponse is not in compliance with the protocol if any listed header\nis missing from the serialized response or if the expected header\nvalue differs from the serialized response value.\n\n`headers` applies no constraints on additional headers.")),
    StringList.underlyingSchema.optional[HttpResponseTestCase]("forbidHeaders", _.forbidHeaders).addHints(smithy.api.Documentation("A list of header field names that must not appear.")),
    StringList.underlyingSchema.optional[HttpResponseTestCase]("requireHeaders", _.requireHeaders).addHints(smithy.api.Documentation("A list of header field names that must appear in the serialized\nHTTP message, but no assertion is made on the value.\n\nHeaders listed in `headers` map do not need to appear in this list.")),
    string.optional[HttpResponseTestCase]("body", _.body).addHints(smithy.api.Documentation("Defines the HTTP message body.\n\nIf no response body is defined, then no assertions are made about\nthe body of the message.")),
    string.optional[HttpResponseTestCase]("bodyMediaType", _.bodyMediaType).addHints(smithy.api.Documentation("The media type of the `body`.\n\nThis is used to help test runners to parse and validate the expected\ndata against generated data. Binary media type formats require that\nthe contents of `body` are base64 encoded.")),
    document.optional[HttpResponseTestCase]("params", _.params).addHints(smithy.api.Documentation("Defines the output parameters deserialized from the HTTP response.\n\nThese parameters MUST be compatible with the output of the operation.")),
    document.optional[HttpResponseTestCase]("vendorParams", _.vendorParams).addHints(smithy.api.Documentation("Defines vendor-specific parameters that are used to influence the\nresponse. For example, some vendors might utilize environment\nvariables, configuration files on disk, or other means to influence\nthe serialization formats used by clients or servers.\n\nIf a `vendorParamsShape` is set, these parameters MUST be compatible\nwith that shape\'s definition.")),
    string.optional[HttpResponseTestCase]("vendorParamsShape", _.vendorParamsShape).addHints(smithy.api.Documentation("A shape to be used to validate the `vendorParams` member contents.\n\nIf set, the parameters in `vendorParams` MUST be compatible with this\nshape\'s definition."), smithy.api.IdRef(selector = "*", failWhenMissing = Some(true), errorMessage = None)),
    string.optional[HttpResponseTestCase]("documentation", _.documentation).addHints(smithy.api.Documentation("A description of the test and what is being asserted.")),
    NonEmptyStringList.underlyingSchema.optional[HttpResponseTestCase]("tags", _.tags).addHints(smithy.api.Documentation("Applies a list of tags to the test.")),
    AppliesTo.schema.optional[HttpResponseTestCase]("appliesTo", _.appliesTo).addHints(smithy.api.Documentation("Indicates that the test case is only to be implemented by \"client\" or\n\"server\" implementations. This property is useful for identifying and\ntesting edge cases of clients and servers that are impossible or\nundesirable to test in *both* client and server implementations.")),
  ){
    HttpResponseTestCase.apply
  }.withId(id).addHints(hints)
}
