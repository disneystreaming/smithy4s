package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person(name: String, town: Option[String] = None, tags: Option[List[String]] = None)

object Person extends ShapeTag.Companion[Person] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "Person")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: String, town: Option[String], tags: Option[List[String]]): Person = Person(name, town, tags)

  implicit val schema: Schema[Person] = struct[Person](
    string.required[Person]("name", _.name).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
    string.optional[Person]("town", _.town).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("town"))),
    list(string).optional[Person]("tags", _.tags).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("tags"))),
  )(make).withId(id).addHints(hints)
}
