package smithy4s.example

import _root_.smithy4s.Document
import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.document

object ArbitraryData extends Newtype[Document] {
  val id: ShapeId = ShapeId("smithy4s.example", "arbitraryData")
  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
  val underlyingSchema: Schema[Document] = document.withId(id).addHints(hints)
  implicit val schema: Schema[ArbitraryData] = recursive(bijection(underlyingSchema, asBijection))
}
