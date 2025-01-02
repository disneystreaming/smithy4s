package smithy4s.example._package

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string

object MyPackageStringTrait extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.package", "MyPackageStringTrait")
  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[MyPackageStringTrait] = recursive(bijection(underlyingSchema, asBijection))
}
