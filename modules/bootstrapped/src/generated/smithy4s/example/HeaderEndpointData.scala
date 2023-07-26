package smithy4s.example

import smithy.api.HttpHeader
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HeaderEndpointData(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None)
object HeaderEndpointData extends ShapeTag.$Companion[HeaderEndpointData] {
  val $id: ShapeId = ShapeId("smithy4s.example", "HeaderEndpointData")

  val $hints: Hints = Hints.empty

  val uppercaseHeader: FieldLens[HeaderEndpointData, Option[String]] = string.optional[HeaderEndpointData]("uppercaseHeader", _.uppercaseHeader, n => c => c.copy(uppercaseHeader = n)).addHints(HttpHeader("X-UPPERCASE-HEADER"))
  val capitalizedHeader: FieldLens[HeaderEndpointData, Option[String]] = string.optional[HeaderEndpointData]("capitalizedHeader", _.capitalizedHeader, n => c => c.copy(capitalizedHeader = n)).addHints(HttpHeader("X-Capitalized-Header"))
  val lowercaseHeader: FieldLens[HeaderEndpointData, Option[String]] = string.optional[HeaderEndpointData]("lowercaseHeader", _.lowercaseHeader, n => c => c.copy(lowercaseHeader = n)).addHints(HttpHeader("x-lowercase-header"))
  val mixedHeader: FieldLens[HeaderEndpointData, Option[String]] = string.optional[HeaderEndpointData]("mixedHeader", _.mixedHeader, n => c => c.copy(mixedHeader = n)).addHints(HttpHeader("x-MiXeD-hEaDEr"))

  implicit val $schema: Schema[HeaderEndpointData] = struct(
    uppercaseHeader,
    capitalizedHeader,
    lowercaseHeader,
    mixedHeader,
  ){
    HeaderEndpointData.apply
  }.withId($id).addHints($hints)
}
