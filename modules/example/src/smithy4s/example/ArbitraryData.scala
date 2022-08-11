package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object ArbitraryData extends Newtype[Document] {
  val id: ShapeId = ShapeId("smithy4s.example", "arbitraryData")
  val hints : Hints = Hints(
    smithy.api.Trait(None, None, None, None),
  )
  val underlyingSchema : Schema[Document] = document.withId(id).addHints(hints)
  implicit val schema : Schema[ArbitraryData] = bijection(underlyingSchema, asBijection)
}