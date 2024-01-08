package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object ReservedKeywordTraitExampleCollection extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedKeywordTraitExampleCollection")
  val hints: Hints = Hints(
    smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))),
  )
  val underlyingSchema: Schema[List[String]] = list(String.schema.addMemberHints(smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))))).withId(id).addHints(hints)
  implicit val schema: Schema[ReservedKeywordTraitExampleCollection] = bijection(underlyingSchema, asBijection)
}
