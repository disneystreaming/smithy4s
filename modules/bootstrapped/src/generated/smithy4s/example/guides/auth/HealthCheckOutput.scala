package smithy4s.example.guides.auth

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HealthCheckOutput(message: String)

object HealthCheckOutput extends ShapeTag.Companion[HealthCheckOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "HealthCheckOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[HealthCheckOutput] = struct(
    string.required[HealthCheckOutput]("message", _.message),
  ){
    HealthCheckOutput.apply
  }.withId(id).addHints(hints)
}
