package smithy4s.example.guides.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthCheckOutput(message: String)
object HealthCheckOutput extends ShapeTag.Companion[HealthCheckOutput] {
  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  val message = string.required[HealthCheckOutput]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[HealthCheckOutput] = struct(
    message,
  ){
    HealthCheckOutput.apply
  }.withId(ShapeId("smithy4s.example.guides.auth", "HealthCheckOutput")).addHints(hints)
}
