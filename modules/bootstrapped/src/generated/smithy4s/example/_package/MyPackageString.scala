package smithy4s.example._package

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object MyPackageString extends Newtype[String] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(ShapeId("smithy4s.example.package", "MyPackageString")).addHints(hints)
  implicit val schema: Schema[MyPackageString] = bijection(underlyingSchema, asBijection)
}
