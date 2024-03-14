package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class StringListWrapper(strings: List[String], wrappedStrings: List[String])

object StringListWrapper extends ShapeTag.Companion[StringListWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "StringListWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(strings: List[String], wrappedStrings: List[String]): StringListWrapper = StringListWrapper(strings, wrappedStrings)

  implicit val schema: Schema[StringListWrapper] = struct(
    StringList.underlyingSchema.required[StringListWrapper]("strings", _.strings),
    WrappedStringList.underlyingSchema.required[StringListWrapper]("wrappedStrings", _.wrappedStrings),
  )(make).withId(id).addHints(hints)
}
