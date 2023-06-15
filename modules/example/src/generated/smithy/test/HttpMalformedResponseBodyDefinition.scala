package smithy.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param assertion
  *   The assertion to execute against the response body.
  * @param mediaType
  *   The media type of the response body.
  *   
  *   This is used to help test runners to parse and evaluate
  *   `contents' and `messageRegex` in the assertion
  */
final case class HttpMalformedResponseBodyDefinition(assertion: HttpMalformedResponseBodyAssertion, mediaType: String)
object HttpMalformedResponseBodyDefinition extends ShapeTag.Companion[HttpMalformedResponseBodyDefinition] {
  val id: ShapeId = ShapeId("smithy.test", "HttpMalformedResponseBodyDefinition")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  implicit val schema: Schema[HttpMalformedResponseBodyDefinition] = struct(
    HttpMalformedResponseBodyAssertion.schema.required[HttpMalformedResponseBodyDefinition]("assertion", _.assertion).addHints(smithy.api.Documentation("The assertion to execute against the response body."), smithy.api.Required()),
    string.required[HttpMalformedResponseBodyDefinition]("mediaType", _.mediaType).addHints(smithy.api.Documentation("The media type of the response body.\n\nThis is used to help test runners to parse and evaluate\n`contents\' and `messageRegex` in the assertion"), smithy.api.Required()),
  ){
    HttpMalformedResponseBodyDefinition.apply
  }.withId(id).addHints(hints)
}
