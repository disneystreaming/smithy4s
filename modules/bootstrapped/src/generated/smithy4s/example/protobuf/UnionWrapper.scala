package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class UnionWrapper(myUnion: Option[MyUnion] = None)

object UnionWrapper extends ShapeTag.Companion[UnionWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "UnionWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(myUnion: Option[MyUnion]): UnionWrapper = UnionWrapper(myUnion)

  implicit val schema: Schema[UnionWrapper] = struct(
    MyUnion.schema.optional[UnionWrapper]("myUnion", _.myUnion),
  )(make).withId(id).addHints(hints)
}
