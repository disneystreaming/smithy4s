package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Person(name: String, town: Option[String] = None)
object Person extends ShapeTag.Companion[Person] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "Person")

  val hints: Hints = Hints.empty

  object Lenses {
    val name = Lens[Person, String](_.name)(n => a => a.copy(name = n))
    val town = Lens[Person, Option[String]](_.town)(n => a => a.copy(town = n))
  }

  implicit val schema: Schema[Person] = struct(
    string.required[Person]("name", _.name).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
    string.optional[Person]("town", _.town).addHints(smithy.api.HttpQuery("town")),
  ){
    Person.apply
  }.withId(id).addHints(hints)
}
