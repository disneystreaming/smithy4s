package smithy4s.example

import smithy.api.Trait
import smithy4s.Document
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.recursive

object FreeForm extends Newtype[Document] {
  val underlyingSchema: Schema[Document] = document
  .withId(ShapeId("smithy4s.example", "freeForm"))
  .addHints(
    Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[FreeForm] = recursive(bijection(underlyingSchema, asBijection))
}
