package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthRequest(query: Option[String] = None)
object HealthRequest extends ShapeTag.Companion[HealthRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "HealthRequest")

  val hints: Hints = Hints.empty

  object Lenses {
    val query = Lens[HealthRequest, Option[String]](_.query)(n => a => a.copy(query = n))
  }

  implicit val schema: Schema[HealthRequest] = struct(
    string.validated(smithy.api.Length(min = Some(0L), max = Some(5L))).optional[HealthRequest]("query", _.query).addHints(smithy.api.HttpQuery("query")),
  ){
    HealthRequest.apply
  }.withId(id).addHints(hints)
}
