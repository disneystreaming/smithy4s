package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class SparseQueryInput(foo: List[Option[String]])

object SparseQueryInput extends ShapeTag.Companion[SparseQueryInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "SparseQueryInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(foo: List[Option[String]]): SparseQueryInput = SparseQueryInput(foo)

  implicit val schema: Schema[SparseQueryInput] = struct(
    SparseStringList.underlyingSchema.required[SparseQueryInput]("foo", _.foo).addHints(smithy.api.HttpQuery("foo")),
  )(make).withId(id).addHints(hints)
}
