package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person2(item: Option[String] = None)

object Person2 extends ShapeTag.Companion[Person2] {
  val id: ShapeId = ShapeId("weather", "Person2")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(item: Option[String]): Person2 = Person2(item)

  implicit val schema: Schema[Person2] = struct(
    string.optional[Person2]("item", _.item),
  )(make).withId(id).addHints(hints)
}
