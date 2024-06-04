package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class SparseQueryOutput(foo: List[Option[String]])

object SparseQueryOutput extends ShapeTag.Companion[SparseQueryOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "SparseQueryOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(foo: List[Option[String]]): SparseQueryOutput = SparseQueryOutput(foo)

  implicit val schema: Schema[SparseQueryOutput] = struct(
    SparseFooList.underlyingSchema.required[SparseQueryOutput]("foo", _.foo),
  )(make).withId(id).addHints(hints)
}
