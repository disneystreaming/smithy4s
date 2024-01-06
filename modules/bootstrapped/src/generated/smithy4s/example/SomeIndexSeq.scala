package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.indexedSeq
import smithy4s.schema.Schema.string

object SomeIndexSeq extends Newtype[IndexedSeq[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeIndexSeq")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[IndexedSeq[String]] = indexedSeq(string).withId(id).addHints(hints)
  implicit val schema: Schema[SomeIndexSeq] = bijection(underlyingSchema, asBijection)
}
