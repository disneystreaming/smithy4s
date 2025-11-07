package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetIntEnumInput(aa: EnumResult)

object GetIntEnumInput extends ShapeTag.Companion[GetIntEnumInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetIntEnumInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(aa: EnumResult): GetIntEnumInput = GetIntEnumInput(aa)

  implicit val schema: Schema[GetIntEnumInput] = struct(
    EnumResult.schema.required[GetIntEnumInput]("aa", _.aa).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
