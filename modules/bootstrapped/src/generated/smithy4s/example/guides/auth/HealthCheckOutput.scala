package smithy4s.example.guides.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthCheckOutput(message: String)
object HealthCheckOutput extends ShapeTag.Companion[HealthCheckOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "HealthCheckOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  object Optics {
    val message = Lens[HealthCheckOutput, String](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[HealthCheckOutput] = struct(
    string.required[HealthCheckOutput]("message", _.message).addHints(smithy.api.Required()),
  ){
    HealthCheckOutput.apply
  }.withId(id).addHints(hints)
}
