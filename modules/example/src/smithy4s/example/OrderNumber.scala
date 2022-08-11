package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object OrderNumber extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderNumber")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema : Schema[OrderNumber] = bijection(underlyingSchema, asBijection)
}