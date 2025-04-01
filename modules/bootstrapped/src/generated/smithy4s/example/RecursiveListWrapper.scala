package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class RecursiveListWrapper(items: List[smithy4s.example.RecursiveListWrapper])

object RecursiveListWrapper extends ShapeTag.Companion[RecursiveListWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveListWrapper")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(items: List[smithy4s.example.RecursiveListWrapper]): RecursiveListWrapper = RecursiveListWrapper(items)

  implicit val schema: Schema[RecursiveListWrapper] = recursive(struct(
    RecursiveList.underlyingSchema.required[RecursiveListWrapper]("items", _.items),
  )(make).withId(id).addHints(hints))
}
