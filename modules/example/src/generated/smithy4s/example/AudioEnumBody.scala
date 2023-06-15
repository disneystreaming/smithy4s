package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class AudioEnumBody(str: AudioEnum)
object AudioEnumBody extends ShapeTag.Companion[AudioEnumBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "AudioEnumBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AudioEnumBody] = struct(
    AudioEnum.schema.required[AudioEnumBody]("str", _.str).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    AudioEnumBody.apply
  }.withId(id).addHints(hints)
}
