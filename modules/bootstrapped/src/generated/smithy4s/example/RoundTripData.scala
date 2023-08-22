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

  implicit val schema: Schema[RoundTripData] = struct(
    string.required[RoundTripData]("label", _.label).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.optional[RoundTripData]("header", _.header).addHints(smithy.api.HttpHeader("HEADER")),
    string.optional[RoundTripData]("query", _.query).addHints(smithy.api.HttpQuery("query")),
    string.optional[RoundTripData]("body", _.body),
  ){
    RoundTripData.apply
  }.withId(id).addHints(hints)
}
