package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Integers(int: Int, sint: Int, uint: Int, fixedUint: Int, fixedSint: Int)

object Integers extends ShapeTag.Companion[Integers] {
  val id: ShapeId = ShapeId("smithy4s.example", "Integers")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(int: Int, sint: Int, uint: Int, fixedUint: Int, fixedSint: Int): Integers = Integers(int, sint, uint, fixedUint, fixedSint)

  implicit val schema: Schema[Integers] = struct(
    int.required[Integers]("int", _.int).addHints(alloy.proto.ProtoIndex(1)),
    int.required[Integers]("sint", _.sint).addHints(alloy.proto.ProtoNumType.SIGNED.widen, alloy.proto.ProtoIndex(2)),
    int.required[Integers]("uint", _.uint).addHints(alloy.proto.ProtoNumType.UNSIGNED.widen, alloy.proto.ProtoIndex(3)),
    int.required[Integers]("fixedUint", _.fixedUint).addHints(alloy.proto.ProtoNumType.FIXED.widen, alloy.proto.ProtoIndex(4)),
    int.required[Integers]("fixedSint", _.fixedSint).addHints(alloy.proto.ProtoNumType.FIXED_SIGNED.widen, alloy.proto.ProtoIndex(5)),
  )(make).withId(id).addHints(hints)
}
