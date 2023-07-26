package smithy4s.example

import smithy.api.HttpQuery
import smithy.api.Length
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthRequest(query: Option[String] = None)
object HealthRequest extends ShapeTag.Companion[HealthRequest] {

  val query = string.validated(Length(min = Some(0L), max = Some(5L))).optional[HealthRequest]("query", _.query, n => c => c.copy(query = n)).addHints(HttpQuery("query"))

  implicit val schema: Schema[HealthRequest] = struct(
    query,
  ){
    HealthRequest.apply
  }
  .withId(ShapeId("smithy4s.example", "HealthRequest"))
  .addHints(
    Hints.empty
  )
}
