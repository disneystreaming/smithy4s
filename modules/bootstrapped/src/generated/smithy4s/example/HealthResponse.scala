package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthResponse(status: String)
object HealthResponse extends ShapeTag.Companion[HealthResponse] {
  val hints: Hints = Hints(
    smithy4s.example.FreeForm(smithy4s.Document.obj("i" -> smithy4s.Document.fromDouble(1.0d), "a" -> smithy4s.Document.fromDouble(2.0d))),
  )

  val status = string.required[HealthResponse]("status", _.status).addHints(smithy.api.Required())

  implicit val schema: Schema[HealthResponse] = struct(
    status,
  ){
    HealthResponse.apply
  }.withId(ShapeId("smithy4s.example", "HealthResponse")).addHints(hints)
}
