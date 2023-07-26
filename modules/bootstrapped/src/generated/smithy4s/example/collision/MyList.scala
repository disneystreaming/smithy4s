package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object MyList extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "MyList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(String.schema).withId(id).addHints(hints)
  implicit val schema: Schema[MyList] = bijection(underlyingSchema, asBijection)
}
