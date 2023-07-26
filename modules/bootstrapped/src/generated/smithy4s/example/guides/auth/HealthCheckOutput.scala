package smithy4s.example.guides.auth

import smithy.api.Output
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthCheckOutput(message: String)
object HealthCheckOutput extends ShapeTag.$Companion[HealthCheckOutput] {
  val $id: ShapeId = ShapeId("smithy4s.example.guides.auth", "HealthCheckOutput")

  val $hints: Hints = Hints(
    Output(),
  )

  val message: FieldLens[HealthCheckOutput, String] = string.required[HealthCheckOutput]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val $schema: Schema[HealthCheckOutput] = struct(
    message,
  ){
    HealthCheckOutput.apply
  }.withId($id).addHints($hints)
}
