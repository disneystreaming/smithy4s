package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class MyIntListWrapper(ints: List[MyInt])

object MyIntListWrapper extends ShapeTag.Companion[MyIntListWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "MyIntListWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(ints: List[MyInt]): MyIntListWrapper = MyIntListWrapper(ints)

  implicit val schema: Schema[MyIntListWrapper] = struct(
    MyIntList.underlyingSchema.required[MyIntListWrapper]("ints", _.ints),
  )(make).withId(id).addHints(hints)
}
