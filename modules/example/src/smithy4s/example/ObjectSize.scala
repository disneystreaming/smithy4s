package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object ObjectSize extends Newtype[Int] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ObjectSize")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[ObjectSize] = bijection(underlyingSchema, ObjectSize(_), (_ : ObjectSize).value)
}