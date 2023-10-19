package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestIdRef(test: Option[ShapeId] = None, test2: Option[TestIdRefTwo] = None)

object TestIdRef extends ShapeTag.Companion[TestIdRef] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRef")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestIdRef] = struct(
    string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).optional[TestIdRef]("test", _.test),
    TestIdRefTwo.schema.optional[TestIdRef]("test2", _.test2),
  ){
    TestIdRef.apply
  }.withId(id).addHints(hints)
}
