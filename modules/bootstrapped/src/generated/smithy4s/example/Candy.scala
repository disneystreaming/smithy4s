package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Candy(name: Option[String] = None)

object Candy extends ShapeTag.Companion[Candy] {
  val id: ShapeId = ShapeId("smithy4s.example", "Candy")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Candy] = struct(
    string.optional[Candy]("name", _.name),
  ){
    Candy.apply
  }.withId(id).addHints(hints)
}
