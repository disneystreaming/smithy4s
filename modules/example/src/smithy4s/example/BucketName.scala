package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object BucketName extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "BucketName")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[BucketName] = bijection(underlyingSchema, asBijection)
}
