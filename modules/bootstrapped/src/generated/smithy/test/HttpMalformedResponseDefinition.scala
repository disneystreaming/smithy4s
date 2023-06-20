package smithy.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

/** @param headers
  *   Defines a map of expected HTTP headers.
  *   
  *   Headers that are not listed in this map are ignored.
  * @param code
  *   Defines the HTTP response code.
  * @param body
  *   The expected response body.
  */
final case class HttpMalformedResponseDefinition(code: Int, headers: Option[Map[String, String]] = None, body: Option[HttpMalformedResponseBodyDefinition] = None)
object HttpMalformedResponseDefinition extends ShapeTag.Companion[HttpMalformedResponseDefinition] {
  val id: ShapeId = ShapeId("smithy.test", "HttpMalformedResponseDefinition")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  implicit val schema: Schema[HttpMalformedResponseDefinition] = struct(
    int.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(100.0)), max = Some(scala.math.BigDecimal(599.0)))).required[HttpMalformedResponseDefinition]("code", _.code).addHints(smithy.api.Required(), smithy.api.Documentation("Defines the HTTP response code.")),
    StringMap.underlyingSchema.optional[HttpMalformedResponseDefinition]("headers", _.headers).addHints(smithy.api.Documentation("Defines a map of expected HTTP headers.\n\nHeaders that are not listed in this map are ignored.")),
    HttpMalformedResponseBodyDefinition.schema.optional[HttpMalformedResponseDefinition]("body", _.body).addHints(smithy.api.Documentation("The expected response body.")),
  ){
    HttpMalformedResponseDefinition.apply
  }.withId(id).addHints(hints)
}
