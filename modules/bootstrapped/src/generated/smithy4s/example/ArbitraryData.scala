package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.recursive

object ArbitraryData extends Newtype[Document] {
  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
  val underlyingSchema: Schema[Document] = document.withId(ShapeId("smithy4s.example", "arbitraryData")).addHints(hints)
  implicit val schema: Schema[ArbitraryData] = recursive(bijection(underlyingSchema, asBijection))
}
