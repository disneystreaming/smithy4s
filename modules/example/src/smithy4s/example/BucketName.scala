package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.syntax._

object BucketName extends Newtype[String] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "BucketName")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[BucketName] = bijection(underlyingSchema, BucketName(_), (_ : BucketName).value)
}