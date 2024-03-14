package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class StringMapWrapper(values: Map[String, Int])

object StringMapWrapper extends ShapeTag.Companion[StringMapWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "StringMapWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(values: Map[String, Int]): StringMapWrapper = StringMapWrapper(values)

  implicit val schema: Schema[StringMapWrapper] = struct(
    StringMap.underlyingSchema.required[StringMapWrapper]("values", _.values),
  )(make).withId(id).addHints(hints)
}
