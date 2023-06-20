package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object StringList extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy.test", "StringList")
  val hints: Hints = Hints(
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema: Schema[StringList] = bijection(underlyingSchema, asBijection)
}
