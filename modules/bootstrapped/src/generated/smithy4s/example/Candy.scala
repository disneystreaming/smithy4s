package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Candy(name: Option[String] = None)
object Candy extends ShapeTag.Companion[Candy] {

  val name = string.optional[Candy]("name", _.name, n => c => c.copy(name = n))

  implicit val schema: Schema[Candy] = struct(
    name,
  ){
    Candy.apply
  }
  .withId(ShapeId("smithy4s.example", "Candy"))
  .addHints(
    Hints.empty
  )
}
