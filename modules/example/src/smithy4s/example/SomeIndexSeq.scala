package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object SomeIndexSeq extends Newtype[IndexedSeq[String]] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "SomeIndexSeq")
  val hints : smithy4s.Hints = smithy4s.Hints()
  val underlyingSchema : smithy4s.Schema[IndexedSeq[String]] = indexedSeq(string).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[SomeIndexSeq] = bijection(underlyingSchema, SomeIndexSeq.make, (_ : SomeIndexSeq).value)
}