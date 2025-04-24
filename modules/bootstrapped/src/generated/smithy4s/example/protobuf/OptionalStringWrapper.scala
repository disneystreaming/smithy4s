package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class OptionalStringWrapper(string: Option[String] = None)

object OptionalStringWrapper extends ShapeTag.Companion[OptionalStringWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OptionalStringWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(string: Option[String]): OptionalStringWrapper = OptionalStringWrapper(string)

  implicit val schema: Schema[OptionalStringWrapper] = struct(
    string.optional[OptionalStringWrapper]("string", _.string),
  )(make).withId(id).addHints(hints)
}
