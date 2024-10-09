package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestReservedNamespaceTrait extends Newtype[java.lang.String] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "TestReservedNamespaceTrait")
  val hints: Hints = Hints(
    smithy4s.example._package.MyPackageStringTrait("test"),
  ).lazily
  val underlyingSchema: Schema[java.lang.String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[TestReservedNamespaceTrait] = bijection(underlyingSchema, asBijection)
}
