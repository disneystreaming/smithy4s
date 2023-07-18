package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HeaderEndpointData(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None)
object HeaderEndpointData extends ShapeTag.Companion[HeaderEndpointData] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeaderEndpointData")

  val hints: Hints = Hints.empty

  object Lenses {
    val uppercaseHeader = Lens[HeaderEndpointData, Option[String]](_.uppercaseHeader)(n => a => a.copy(uppercaseHeader = n))
    val capitalizedHeader = Lens[HeaderEndpointData, Option[String]](_.capitalizedHeader)(n => a => a.copy(capitalizedHeader = n))
    val lowercaseHeader = Lens[HeaderEndpointData, Option[String]](_.lowercaseHeader)(n => a => a.copy(lowercaseHeader = n))
    val mixedHeader = Lens[HeaderEndpointData, Option[String]](_.mixedHeader)(n => a => a.copy(mixedHeader = n))
  }

  implicit val schema: Schema[HeaderEndpointData] = struct(
    string.optional[HeaderEndpointData]("uppercaseHeader", _.uppercaseHeader).addHints(smithy.api.HttpHeader("X-UPPERCASE-HEADER")),
    string.optional[HeaderEndpointData]("capitalizedHeader", _.capitalizedHeader).addHints(smithy.api.HttpHeader("X-Capitalized-Header")),
    string.optional[HeaderEndpointData]("lowercaseHeader", _.lowercaseHeader).addHints(smithy.api.HttpHeader("x-lowercase-header")),
    string.optional[HeaderEndpointData]("mixedHeader", _.mixedHeader).addHints(smithy.api.HttpHeader("x-MiXeD-hEaDEr")),
  ){
    HeaderEndpointData.apply
  }.withId(id).addHints(hints)
}
