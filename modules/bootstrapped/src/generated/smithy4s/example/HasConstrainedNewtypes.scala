package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class HasConstrainedNewtypes(a: BucketName, b: CityId, c: Option[ObjectSize] = None, d: Option[IndexedSeq[String]] = None, e: Option[PNG] = None)

object HasConstrainedNewtypes extends ShapeTag.Companion[HasConstrainedNewtypes] {
  val id: ShapeId = ShapeId("smithy4s.example", "HasConstrainedNewtypes")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(a: BucketName, b: CityId, c: Option[ObjectSize], d: Option[IndexedSeq[String]], e: Option[PNG]): HasConstrainedNewtypes = HasConstrainedNewtypes(a, b, c, d, e)

  implicit val schema: Schema[HasConstrainedNewtypes] = struct(
    BucketName.schema.validated(smithy.api.Length(min = Some(1L), max = None)).required[HasConstrainedNewtypes]("a", _.a),
    CityId.schema.validated(smithy.api.Length(min = Some(1L), max = None)).required[HasConstrainedNewtypes]("b", _.b),
    ObjectSize.schema.validated(smithy.api.Range(min = Some(scala.math.BigDecimal(1.0)), max = None)).optional[HasConstrainedNewtypes]("c", _.c),
    SomeIndexSeq.underlyingSchema.validated(smithy.api.Length(min = Some(1L), max = None)).optional[HasConstrainedNewtypes]("d", _.d),
    PNG.schema.validated(smithy.api.Length(min = Some(1L), max = None)).optional[HasConstrainedNewtypes]("e", _.e),
  )(make).withId(id).addHints(hints)
}
