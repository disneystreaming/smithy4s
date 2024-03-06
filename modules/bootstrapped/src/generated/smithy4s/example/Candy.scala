package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Candy(name: Option[String] = None)

object Candy extends ShapeTag.Companion[Candy] {
  val id: ShapeId = ShapeId("smithy4s.example", "Candy")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: Option[String]): Candy = Candy(name)

  implicit val schema: Schema[Candy] = struct(
    string.optional[Candy]("name", _.name),
  )(make).withId(id).addHints(hints)
}
