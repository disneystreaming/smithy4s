package smithy4s.example.hello

import smithy.api.HttpLabel
import smithy.api.HttpQuery
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person(name: String, town: Option[String] = None)
object Person extends ShapeTag.Companion[Person] {

  val name = string.required[Person]("name", _.name, n => c => c.copy(name = n)).addHints(HttpLabel(), Required())
  val town = string.optional[Person]("town", _.town, n => c => c.copy(town = n)).addHints(HttpQuery("town"))

  implicit val schema: Schema[Person] = struct(
    name,
    town,
  ){
    Person.apply
  }
  .withId(ShapeId("smithy4s.example.hello", "Person"))
  .addHints(
    Hints.empty
  )
}
