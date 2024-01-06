package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class TestIdRef(test: Option[smithy4s.ShapeId] = None, test2: Option[TestIdRefTwo] = None)

object TestIdRef extends ShapeTag.Companion[TestIdRef] {
  val id: _root_.smithy4s.ShapeId = _root_.smithy4s.ShapeId("smithy4s.example", "TestIdRef")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestIdRef] = struct(
    string.refined[smithy4s.ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).optional[TestIdRef]("test", _.test),
    TestIdRefTwo.schema.optional[TestIdRef]("test2", _.test2),
  ){
    TestIdRef.apply
  }.withId(id).addHints(hints)
}
