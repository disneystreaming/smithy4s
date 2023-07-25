package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RoundTripData(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None)
object RoundTripData extends ShapeTag.Companion[RoundTripData] {
  val id: ShapeId = ShapeId("smithy4s.example", "RoundTripData")

  val hints: Hints = Hints.empty

  val label = string.required[RoundTripData]("label", _.label).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val header = string.optional[RoundTripData]("header", _.header).addHints(smithy.api.HttpHeader("HEADER"))
  val query = string.optional[RoundTripData]("query", _.query).addHints(smithy.api.HttpQuery("query"))
  val body = string.optional[RoundTripData]("body", _.body)

  implicit val schema: Schema[RoundTripData] = struct(
    label,
    header,
    query,
    body,
  ){
    RoundTripData.apply
  }.withId(id).addHints(hints)
}
