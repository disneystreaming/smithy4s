package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ReservedKeywordTraitExamplePrimitive extends Newtype[java.lang.String] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedKeywordTraitExamplePrimitive")
  val hints: Hints = Hints(
    smithy4s.example.collision.ReservedKeywordStructTrait(_implicit = smithy4s.example.collision.String("demo"), _package = Some(smithy4s.example.collision.Packagee(_class = Some(42)))),
  )
  val underlyingSchema: Schema[java.lang.String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[ReservedKeywordTraitExamplePrimitive] = bijection(underlyingSchema, asBijection)
}
