package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthResponse(status: String)
object HealthResponse extends ShapeTag.Companion[HealthResponse] {

  val status: FieldLens[HealthResponse, String] = string.required[HealthResponse]("status", _.status, n => c => c.copy(status = n)).addHints(Required())

  implicit val schema: Schema[HealthResponse] = struct(
    status,
  ){
    HealthResponse.apply
  }
  .withId(ShapeId("smithy4s.example", "HealthResponse"))
  .addHints(
    FreeForm(smithy4s.Document.obj("i" -> smithy4s.Document.fromDouble(1.0d), "a" -> smithy4s.Document.fromDouble(2.0d))),
  )
}
