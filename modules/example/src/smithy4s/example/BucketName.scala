package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object BucketName extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "BucketName")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : Schema[BucketName] = bijection(underlyingSchema, asBijection)
}