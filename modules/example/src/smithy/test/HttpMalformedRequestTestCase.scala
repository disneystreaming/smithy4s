package smithy.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param request
  *   The malformed request to send.
  * @param tags
  *   Applies a list of tags to the test.
  * @param documentation
  *   A description of the test and what is being asserted.
  * @param testParameters
  *   An optional set of test parameters for parameterized testing.
  * @param protocol
  *   The name of the protocol to test.
  * @param response
  *   The expected response.
  * @param id
  *   The identifier of the test case. This identifier can be used by
  *   protocol test implementations to filter out unsupported test
  *   cases by ID, to generate test case names, etc. The provided `id`
  *   MUST match Smithy's `identifier` ABNF. No two `httpMalformedRequestTests`
  *   test cases can share the same ID.
  */
final case class HttpMalformedRequestTestCase(id: String, protocol: String, request: HttpMalformedRequestDefinition, response: HttpMalformedResponseDefinition, documentation: Option[String] = None, tags: Option[List[NonEmptyString]] = None, testParameters: Option[Map[String, List[String]]] = None)
object HttpMalformedRequestTestCase extends ShapeTag.Companion[HttpMalformedRequestTestCase] {
  val id: ShapeId = ShapeId("smithy.test", "HttpMalformedRequestTestCase")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  implicit val schema: Schema[HttpMalformedRequestTestCase] = struct(
    string.validated(smithy.api.Pattern("^[A-Za-z_][A-Za-z0-9_]+$")).required[HttpMalformedRequestTestCase]("id", _.id).addHints(smithy.api.Required(), smithy.api.Documentation("The identifier of the test case. This identifier can be used by\nprotocol test implementations to filter out unsupported test\ncases by ID, to generate test case names, etc. The provided `id`\nMUST match Smithy\'s `identifier` ABNF. No two `httpMalformedRequestTests`\ntest cases can share the same ID.")),
    string.required[HttpMalformedRequestTestCase]("protocol", _.protocol).addHints(smithy.api.IdRef(selector = "[trait|protocolDefinition]", failWhenMissing = Some(true), errorMessage = None), smithy.api.Required(), smithy.api.Documentation("The name of the protocol to test.")),
    HttpMalformedRequestDefinition.schema.required[HttpMalformedRequestTestCase]("request", _.request).addHints(smithy.api.Documentation("The malformed request to send."), smithy.api.Required()),
    HttpMalformedResponseDefinition.schema.required[HttpMalformedRequestTestCase]("response", _.response).addHints(smithy.api.Documentation("The expected response."), smithy.api.Required()),
    string.optional[HttpMalformedRequestTestCase]("documentation", _.documentation).addHints(smithy.api.Documentation("A description of the test and what is being asserted.")),
    NonEmptyStringList.underlyingSchema.optional[HttpMalformedRequestTestCase]("tags", _.tags).addHints(smithy.api.Documentation("Applies a list of tags to the test.")),
    HttpMalformedRequestTestParametersDefinition.underlyingSchema.optional[HttpMalformedRequestTestCase]("testParameters", _.testParameters).addHints(smithy.api.Documentation("An optional set of test parameters for parameterized testing.")),
  ){
    HttpMalformedRequestTestCase.apply
  }.withId(id).addHints(hints)
}
