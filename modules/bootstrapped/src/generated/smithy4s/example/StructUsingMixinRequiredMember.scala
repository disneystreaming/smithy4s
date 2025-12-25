package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class StructUsingMixinRequiredMember(description: String, extraField: String) extends MixinRequiredMember

object StructUsingMixinRequiredMember extends ShapeTag.Companion[StructUsingMixinRequiredMember] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructUsingMixinRequiredMember")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(description: String, extraField: String): StructUsingMixinRequiredMember = StructUsingMixinRequiredMember(description, extraField)

  implicit val schema: Schema[StructUsingMixinRequiredMember] = struct(
    string.required[StructUsingMixinRequiredMember]("description", _.description),
    string.required[StructUsingMixinRequiredMember]("extraField", _.extraField),
  )(make).withId(id).addHints(hints)
}
