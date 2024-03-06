package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class StringWrapper(string: String)

object StringWrapper extends ShapeTag.Companion[StringWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(string: String): StringWrapper = StringWrapper(string)

  implicit val schema: Schema[StringWrapper] = struct(
    string.required[StringWrapper]("string", _.string).addHints(alloy.proto.ProtoIndex(1)),
  )(make).withId(id).addHints(hints)
}
