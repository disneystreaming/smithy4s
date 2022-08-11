package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object SomeIndexSeq extends Newtype[IndexedSeq[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeIndexSeq")
  val hints : Hints = Hints()
  val underlyingSchema : Schema[IndexedSeq[String]] = indexedSeq(string).withId(id).addHints(hints)
  implicit val schema : Schema[SomeIndexSeq] = bijection(underlyingSchema, asBijection)
}