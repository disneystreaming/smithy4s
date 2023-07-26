package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object SomeInt extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeInt")
  val hints: Hints = Hints(
    SomeCollections(someList = List("a"), someSet = Set("b"), someMap = Map("a" -> "b")),
    Default(smithy4s.Document.fromDouble(0.0d)),
  )
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema: Schema[SomeInt] = bijection(underlyingSchema, asBijection)
}
