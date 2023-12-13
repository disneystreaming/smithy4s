package smithy4s.example.guides.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthCheckOutput(message: String)

object HealthCheckOutput extends ShapeTag.Companion[HealthCheckOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "HealthCheckOutput")

  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Output(),
    )
  )

  implicit val schema: Schema[HealthCheckOutput] = struct(
    string.required[HealthCheckOutput]("message", _.message),
  ){
    HealthCheckOutput.apply
  }.withId(id).addHints(hints)
}
