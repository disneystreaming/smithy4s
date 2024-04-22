package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Integers(int: Int, sint: Int, uint: Int, fixedUint: Int, fixedSint: Int)

object Integers extends ShapeTag.Companion[Integers] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "Integers")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(int: Int, sint: Int, uint: Int, fixedUint: Int, fixedSint: Int): Integers = Integers(int, sint, uint, fixedUint, fixedSint)

  implicit val schema: Schema[Integers] = struct(
    int.required[Integers]("int", _.int),
    int.required[Integers]("sint", _.sint).addHints(alloy.proto.ProtoNumType.SIGNED.widen),
    int.required[Integers]("uint", _.uint).addHints(alloy.proto.ProtoNumType.UNSIGNED.widen),
    int.required[Integers]("fixedUint", _.fixedUint).addHints(alloy.proto.ProtoNumType.FIXED.widen),
    int.required[Integers]("fixedSint", _.fixedSint).addHints(alloy.proto.ProtoNumType.FIXED_SIGNED.widen),
  )(make).withId(id).addHints(hints)
}
