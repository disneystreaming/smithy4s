package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.int
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object OrderNumber extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderNumber")
  val hints : Hints = Hints(
    smithy.api.Default(smithy4s.Document.fromDouble(0.0)),
  )
  val underlyingSchema : Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema : Schema[OrderNumber] = bijection(underlyingSchema, asBijection)
}