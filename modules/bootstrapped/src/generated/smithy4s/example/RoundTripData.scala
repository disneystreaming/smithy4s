package smithy4s.example

import smithy.api.HttpHeader
import smithy.api.HttpLabel
import smithy.api.HttpQuery
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RoundTripData(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None)
object RoundTripData extends ShapeTag.Companion[RoundTripData] {

  val label: FieldLens[RoundTripData, String] = string.required[RoundTripData]("label", _.label, n => c => c.copy(label = n)).addHints(HttpLabel(), Required())
  val header: FieldLens[RoundTripData, Option[String]] = string.optional[RoundTripData]("header", _.header, n => c => c.copy(header = n)).addHints(HttpHeader("HEADER"))
  val query: FieldLens[RoundTripData, Option[String]] = string.optional[RoundTripData]("query", _.query, n => c => c.copy(query = n)).addHints(HttpQuery("query"))
  val body: FieldLens[RoundTripData, Option[String]] = string.optional[RoundTripData]("body", _.body, n => c => c.copy(body = n))

  implicit val schema: Schema[RoundTripData] = struct(
    label,
    header,
    query,
    body,
  ){
    RoundTripData.apply
  }
  .withId(ShapeId("smithy4s.example", "RoundTripData"))
}
