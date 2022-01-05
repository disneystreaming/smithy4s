package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object BucketName extends Newtype[String] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "BucketName")
  val underlyingSchema : smithy4s.Schema[String] = string
  val schema : smithy4s.Schema[BucketName] = bijection(underlyingSchema, BucketName(_), (_ : BucketName).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[BucketName]] = schematic.Static(schema)
}