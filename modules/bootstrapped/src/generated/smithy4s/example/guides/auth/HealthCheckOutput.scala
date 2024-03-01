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

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: String): HealthCheckOutput = HealthCheckOutput(message)

  implicit val schema: Schema[HealthCheckOutput] = struct(
    string.required[HealthCheckOutput]("message", _.message),
  ){
    make
  }.withId(id).addHints(hints)
}
