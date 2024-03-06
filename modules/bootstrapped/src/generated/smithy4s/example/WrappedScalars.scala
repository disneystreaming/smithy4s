package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class WrappedScalars(int: Option[Int] = None, bool: Option[Boolean] = None)

object WrappedScalars extends ShapeTag.Companion[WrappedScalars] {
  val id: ShapeId = ShapeId("smithy4s.example", "WrappedScalars")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(int: Option[Int], bool: Option[Boolean]): WrappedScalars = WrappedScalars(int, bool)

  implicit val schema: Schema[WrappedScalars] = struct(
    int.optional[WrappedScalars]("int", _.int).addHints(alloy.proto.ProtoWrapped(), alloy.proto.ProtoIndex(1)),
    boolean.optional[WrappedScalars]("bool", _.bool).addHints(alloy.proto.ProtoWrapped(), alloy.proto.ProtoIndex(2)),
  )(make).withId(id).addHints(hints)
}
