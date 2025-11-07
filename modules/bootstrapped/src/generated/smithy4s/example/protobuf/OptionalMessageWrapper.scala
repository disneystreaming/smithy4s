package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class OptionalMessageWrapper(message: Option[Integers] = None)

object OptionalMessageWrapper extends ShapeTag.Companion[OptionalMessageWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OptionalMessageWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(message: Option[Integers]): OptionalMessageWrapper = OptionalMessageWrapper(message)

  implicit val schema: Schema[OptionalMessageWrapper] = struct(
    Integers.schema.optional[OptionalMessageWrapper]("message", _.message),
  )(make).withId(id).addHints(hints)
}
