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

  // constructor using the original order from the spec
  private def make(label: String, header: Option[String], query: Option[String], body: Option[String]): RoundTripData = RoundTripData(label, header, query, body)

  implicit val schema: Schema[RoundTripData] = struct(
    string.required[RoundTripData]("label", _.label).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj()), Hints.dynamic(ShapeId("smithy.api", "suppress"), smithy4s.Document.array(smithy4s.Document.fromString("HttpBindingTraitIgnored.Input")))),
    string.optional[RoundTripData]("header", _.header).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("HEADER"))),
    string.optional[RoundTripData]("query", _.query).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("query")), Hints.dynamic(ShapeId("smithy.api", "suppress"), smithy4s.Document.array(smithy4s.Document.fromString("HttpBindingTraitIgnored.Input")))),
    string.optional[RoundTripData]("body", _.body),
  )(make).withId(id).addHints(hints)
}
