package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object QParams extends Newtype[Map[String, List[String]]] {
  val id: ShapeId = ShapeId("smithy4s.example", "QParams")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, List[String]]] = map(string, QValues.underlyingSchema).withId(id).addHints(hints)
  implicit val schema: Schema[QParams] = bijection(underlyingSchema, asBijection)
}
