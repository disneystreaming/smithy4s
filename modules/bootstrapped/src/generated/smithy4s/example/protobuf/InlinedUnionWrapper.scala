package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class InlinedUnionWrapper(myInlinedUnion: Option[MyInlinedUnion] = None)

object InlinedUnionWrapper extends ShapeTag.Companion[InlinedUnionWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "InlinedUnionWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(myInlinedUnion: Option[MyInlinedUnion]): InlinedUnionWrapper = InlinedUnionWrapper(myInlinedUnion)

  implicit val schema: Schema[InlinedUnionWrapper] = struct(
    MyInlinedUnion.schema.optional[InlinedUnionWrapper]("myInlinedUnion", _.myInlinedUnion),
  )(make).withId(id).addHints(hints)
}
