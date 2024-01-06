package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set

object MySet extends Newtype[Set[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "MySet")
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema: Schema[Set[String]] = set(String.schema).withId(id).addHints(hints)
  implicit val schema: Schema[MySet] = bijection(underlyingSchema, asBijection)
}
