package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object ObjectSize extends Newtype[Int] {
  object T {
    val schema : smithy4s.Schema[Int] = int
    implicit val staticSchema : schematic.Static[smithy4s.Schema[Int]] = schematic.Static(schema)
  }
  def namespace = NAMESPACE
  val name = "ObjectSize"

  val schema : smithy4s.Schema[ObjectSize] = bijection(T.schema, ObjectSize(_), (_ : ObjectSize).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[ObjectSize]] = schematic.Static(schema)
}