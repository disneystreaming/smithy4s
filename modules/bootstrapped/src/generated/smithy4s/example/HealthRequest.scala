package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthRequest(query: Option[String] = None)
object HealthRequest extends ShapeTag.Companion[HealthRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "HealthRequest")

  val hints: Hints = Hints.empty

  val query = string.validated(smithy.api.Length(min = Some(0L), max = Some(5L))).optional[HealthRequest]("query", _.query).addHints(smithy.api.HttpQuery("query"))

  implicit val schema: Schema[HealthRequest] = struct(
    query,
  ){
    HealthRequest.apply
  }.withId(id).addHints(hints)
}
