package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person(name: String, town: Option[String] = None)
object Person extends ShapeTag.Companion[Person] {
  val hints: Hints = Hints.empty

  val name = string.required[Person]("name", _.name).addHints(smithy.api.HttpLabel(), smithy.api.Required())
  val town = string.optional[Person]("town", _.town).addHints(smithy.api.HttpQuery("town"))

  implicit val schema: Schema[Person] = struct(
    name,
    town,
  ){
    Person.apply
  }.withId(ShapeId("smithy4s.example.hello", "Person")).addHints(hints)
}
