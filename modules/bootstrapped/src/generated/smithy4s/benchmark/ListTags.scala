package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object ListTags extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "ListTags")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema: Schema[ListTags] = bijection(underlyingSchema, asBijection)
}
