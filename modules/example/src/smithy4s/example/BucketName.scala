package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object BucketName extends Newtype[String] {
  object T {
    val schema : smithy4s.Schema[String] = string
    implicit val staticSchema : schematic.Static[smithy4s.Schema[String]] = schematic.Static(schema)
  }
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "BucketName")

  val schema : smithy4s.Schema[BucketName] = bijection(T.schema, BucketName(_), (_ : BucketName).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[BucketName]] = schematic.Static(schema)
}