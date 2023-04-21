package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.indexedSeq
import smithy4s.schema.Schema.string

object SomeIndexSeq extends Newtype[IndexedSeq[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeIndexSeq")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[IndexedSeq[String]] = indexedSeq(string).withId(id).addHints(hints)
  implicit val schema: Schema[SomeIndexSeq] = bijection(underlyingSchema, asBijection)

}