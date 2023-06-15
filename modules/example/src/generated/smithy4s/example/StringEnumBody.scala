package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class StringEnumBody(str: StringEnum)
object StringEnumBody extends ShapeTag.Companion[StringEnumBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringEnumBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[StringEnumBody] = struct(
    StringEnum.schema.required[StringEnumBody]("str", _.str).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    StringEnumBody.apply
  }.withId(id).addHints(hints)
}
