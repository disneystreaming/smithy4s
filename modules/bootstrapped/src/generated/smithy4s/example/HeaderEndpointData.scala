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

  val uppercaseHeader = string.optional[HeaderEndpointData]("uppercaseHeader", _.uppercaseHeader).addHints(smithy.api.HttpHeader("X-UPPERCASE-HEADER"))
  val capitalizedHeader = string.optional[HeaderEndpointData]("capitalizedHeader", _.capitalizedHeader).addHints(smithy.api.HttpHeader("X-Capitalized-Header"))
  val lowercaseHeader = string.optional[HeaderEndpointData]("lowercaseHeader", _.lowercaseHeader).addHints(smithy.api.HttpHeader("x-lowercase-header"))
  val mixedHeader = string.optional[HeaderEndpointData]("mixedHeader", _.mixedHeader).addHints(smithy.api.HttpHeader("x-MiXeD-hEaDEr"))

  implicit val schema: Schema[HeaderEndpointData] = struct(
    uppercaseHeader,
    capitalizedHeader,
    lowercaseHeader,
    mixedHeader,
  ){
    HeaderEndpointData.apply
  }.withId(id).addHints(hints)
}
