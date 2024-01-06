package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class RoundTripData(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None)

object RoundTripData extends ShapeTag.Companion[RoundTripData] {
  val id: ShapeId = ShapeId("smithy4s.example", "RoundTripData")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[RoundTripData] = struct(
    string.required[RoundTripData]("label", _.label).addHints(smithy.api.HttpLabel()),
    string.optional[RoundTripData]("header", _.header).addHints(smithy.api.HttpHeader("HEADER")),
    string.optional[RoundTripData]("query", _.query).addHints(smithy.api.HttpQuery("query")),
    string.optional[RoundTripData]("body", _.body),
  ){
    RoundTripData.apply
  }.withId(id).addHints(hints)
}
