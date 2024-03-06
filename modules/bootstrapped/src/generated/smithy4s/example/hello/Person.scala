package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person(name: String, town: Option[String] = None)

object Person extends ShapeTag.Companion[Person] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "Person")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: String, town: Option[String]): Person = Person(name, town)

  implicit val schema: Schema[Person] = struct(
    string.required[Person]("name", _.name).addHints(smithy.api.HttpLabel()),
    string.optional[Person]("town", _.town).addHints(smithy.api.HttpQuery("town")),
  )(make).withId(id).addHints(hints)
}
