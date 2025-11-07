package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class WrappedScalars(int: Option[Int] = None, bool: Option[Boolean] = None)

object WrappedScalars extends ShapeTag.Companion[WrappedScalars] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "WrappedScalars")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(int: Option[Int], bool: Option[Boolean]): WrappedScalars = WrappedScalars(int, bool)

  implicit val schema: Schema[WrappedScalars] = struct(
    int.optional[WrappedScalars]("int", _.int).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoWrapped"), smithy4s.Document.obj())),
    boolean.optional[WrappedScalars]("bool", _.bool).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoWrapped"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
