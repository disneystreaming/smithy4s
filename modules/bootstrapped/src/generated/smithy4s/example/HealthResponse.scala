package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthResponse(status: String)
object HealthResponse extends ShapeTag.Companion[HealthResponse] {
  val id: ShapeId = ShapeId("smithy4s.example", "HealthResponse")

  val hints: Hints = Hints(
    smithy4s.example.FreeForm(smithy4s.Document.obj("i" -> smithy4s.Document.fromDouble(1.0d), "a" -> smithy4s.Document.fromDouble(2.0d))),
  )

  object Optics {
    val status = Lens[HealthResponse, String](_.status)(n => a => a.copy(status = n))
  }

  implicit val schema: Schema[HealthResponse] = struct(
    string.required[HealthResponse]("status", _.status).addHints(smithy.api.Required()),
  ){
    HealthResponse.apply
  }.withId(id).addHints(hints)
}
