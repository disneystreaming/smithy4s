package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person1(item: Option[String] = scala.None)

object Person1 extends ShapeTag.Companion[Person1] {
  val id: ShapeId = ShapeId("weather", "Person1")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(item: Option[String]): Person1 = Person1(item)

  implicit val schema: Schema[Person1] = struct(
    string.optional[Person1]("item", _.item).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
  )(make).withId(id).addHints(hints)
}
