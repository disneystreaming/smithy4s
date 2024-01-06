package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HeaderEndpointData(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None)

object HeaderEndpointData extends ShapeTag.Companion[HeaderEndpointData] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeaderEndpointData")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[HeaderEndpointData] = struct(
    string.optional[HeaderEndpointData]("uppercaseHeader", _.uppercaseHeader).addHints(smithy.api.HttpHeader("X-UPPERCASE-HEADER")),
    string.optional[HeaderEndpointData]("capitalizedHeader", _.capitalizedHeader).addHints(smithy.api.HttpHeader("X-Capitalized-Header")),
    string.optional[HeaderEndpointData]("lowercaseHeader", _.lowercaseHeader).addHints(smithy.api.HttpHeader("x-lowercase-header")),
    string.optional[HeaderEndpointData]("mixedHeader", _.mixedHeader).addHints(smithy.api.HttpHeader("x-MiXeD-hEaDEr")),
  ){
    HeaderEndpointData.apply
  }.withId(id).addHints(hints)
}
