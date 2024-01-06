package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HealthRequest(query: Option[String] = None)

object HealthRequest extends ShapeTag.Companion[HealthRequest] {
  val id: ShapeId = ShapeId("smithy4s.example", "HealthRequest")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[HealthRequest] = struct(
    string.validated(smithy.api.Length(min = Some(0L), max = Some(5L))).optional[HealthRequest]("query", _.query).addHints(smithy.api.HttpQuery("query")),
  ){
    HealthRequest.apply
  }.withId(id).addHints(hints)
}
