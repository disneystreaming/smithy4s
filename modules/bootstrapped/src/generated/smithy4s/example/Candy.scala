package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Candy(name: Option[String] = None)
object Candy extends ShapeTag.Companion[Candy] {
  val id: ShapeId = ShapeId("smithy4s.example", "Candy")

  val hints: Hints = Hints.empty

  object Optics {
    val name = Lens[Candy, Option[String]](_.name)(n => a => a.copy(name = n))
  }

  implicit val schema: Schema[Candy] = struct(
    string.optional[Candy]("name", _.name),
  ){
    Candy.apply
  }.withId(id).addHints(hints)
}
