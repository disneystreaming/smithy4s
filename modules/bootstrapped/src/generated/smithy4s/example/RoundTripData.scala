package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RoundTripData(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None)
object RoundTripData extends ShapeTag.Companion[RoundTripData] {
  val id: ShapeId = ShapeId("smithy4s.example", "RoundTripData")

  val hints: Hints = Hints.empty

  object Optics {
    val label = Lens[RoundTripData, String](_.label)(n => a => a.copy(label = n))
    val header = Lens[RoundTripData, Option[String]](_.header)(n => a => a.copy(header = n))
    val query = Lens[RoundTripData, Option[String]](_.query)(n => a => a.copy(query = n))
    val body = Lens[RoundTripData, Option[String]](_.body)(n => a => a.copy(body = n))
  }

  implicit val schema: Schema[RoundTripData] = struct(
    string.required[RoundTripData]("label", _.label).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.optional[RoundTripData]("header", _.header).addHints(smithy.api.HttpHeader("HEADER")),
    string.optional[RoundTripData]("query", _.query).addHints(smithy.api.HttpQuery("query")),
    string.optional[RoundTripData]("body", _.body),
  ){
    RoundTripData.apply
  }.withId(id).addHints(hints)
}
