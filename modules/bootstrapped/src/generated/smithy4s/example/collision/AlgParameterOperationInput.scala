package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class AlgParameterOperationInput(alg: String)

object AlgParameterOperationInput extends ShapeTag.Companion[AlgParameterOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "AlgParameterOperationInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(alg: String): AlgParameterOperationInput = AlgParameterOperationInput(alg)

  implicit val schema: Schema[AlgParameterOperationInput] = struct(
    String.schema.required[AlgParameterOperationInput]("alg", _.alg),
  )(make).withId(id).addHints(hints)
}
