package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.byte
import smithy4s.schema.Schema.double
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.short
import smithy4s.schema.Schema.struct

final case class OtherScalars(boolean: Boolean, byte: Byte, float: Float, double: Double, short: Short)

object OtherScalars extends ShapeTag.Companion[OtherScalars] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OtherScalars")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(boolean: Boolean, byte: Byte, float: Float, double: Double, short: Short): OtherScalars = OtherScalars(boolean, byte, float, double, short)

  implicit val schema: Schema[OtherScalars] = struct(
    boolean.required[OtherScalars]("boolean", _.boolean),
    byte.required[OtherScalars]("byte", _.byte),
    float.required[OtherScalars]("float", _.float),
    double.required[OtherScalars]("double", _.double),
    short.required[OtherScalars]("short", _.short),
  )(make).withId(id).addHints(hints)
}
