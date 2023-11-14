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
  val id: ShapeId = ShapeId("smithy4s.example", "arbitraryData")
  val hints: Hints = Hints(
    ShapeId("smithy.api", "trait") -> Document.obj(),
  )
  val underlyingSchema: Schema[Document] = document.withId(id).addHints(hints)
  implicit val schema: Schema[ArbitraryData] = recursive(bijection(underlyingSchema, asBijection))
}
