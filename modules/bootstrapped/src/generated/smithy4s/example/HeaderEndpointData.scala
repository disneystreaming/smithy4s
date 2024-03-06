package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HeaderEndpointData(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None)

object HeaderEndpointData extends ShapeTag.Companion[HeaderEndpointData] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeaderEndpointData")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(uppercaseHeader: Option[String], capitalizedHeader: Option[String], lowercaseHeader: Option[String], mixedHeader: Option[String]): HeaderEndpointData = HeaderEndpointData(uppercaseHeader, capitalizedHeader, lowercaseHeader, mixedHeader)

  implicit val schema: Schema[HeaderEndpointData] = struct(
    string.optional[HeaderEndpointData]("uppercaseHeader", _.uppercaseHeader).addHints(smithy.api.HttpHeader("X-UPPERCASE-HEADER")),
    string.optional[HeaderEndpointData]("capitalizedHeader", _.capitalizedHeader).addHints(smithy.api.HttpHeader("X-Capitalized-Header")),
    string.optional[HeaderEndpointData]("lowercaseHeader", _.lowercaseHeader).addHints(smithy.api.HttpHeader("x-lowercase-header")),
    string.optional[HeaderEndpointData]("mixedHeader", _.mixedHeader).addHints(smithy.api.HttpHeader("x-MiXeD-hEaDEr")),
  )(make).withId(id).addHints(hints)
}
