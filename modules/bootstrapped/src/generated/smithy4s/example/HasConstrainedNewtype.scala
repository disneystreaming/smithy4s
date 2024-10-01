package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class HasConstrainedNewtype(s: CityId)

object HasConstrainedNewtype extends ShapeTag.Companion[HasConstrainedNewtype] {
  val id: ShapeId = ShapeId("smithy4s.example", "HasConstrainedNewtype")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(s: CityId): HasConstrainedNewtype = HasConstrainedNewtype(s)

  implicit val schema: Schema[HasConstrainedNewtype] = struct(
    CityId.schema.validated(smithy.api.Length(min = Some(1L), max = None)).required[HasConstrainedNewtype]("s", _.s),
  )(make).withId(id).addHints(hints)
}
