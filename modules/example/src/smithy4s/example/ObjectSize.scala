package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object ObjectSize extends Newtype[Int] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ObjectSize")
  val underlyingSchema : smithy4s.Schema[Int] = int
  val schema : smithy4s.Schema[ObjectSize] = bijection(underlyingSchema, ObjectSize(_), (_ : ObjectSize).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[ObjectSize]] = schematic.Static(schema)
}