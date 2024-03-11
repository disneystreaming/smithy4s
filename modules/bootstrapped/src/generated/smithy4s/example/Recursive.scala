package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class Recursive(recursive: Option[smithy4s.example.Recursive] = None)

object Recursive extends ShapeTag.Companion[Recursive] {
  val id: ShapeId = ShapeId("smithy4s.example", "Recursive")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(recursive: Option[smithy4s.example.Recursive]): Recursive = Recursive(recursive)

  implicit val schema: Schema[Recursive] = recursive(struct(
    smithy4s.example.Recursive.schema.optional[Recursive]("recursive", _.recursive).addHints(alloy.proto.ProtoIndex(1)),
  )(make).withId(id).addHints(hints))
}
