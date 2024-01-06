package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class Serializable()

object Serializable extends ShapeTag.Companion[Serializable] {
  val id: ShapeId = ShapeId("smithy4s.example", "Serializable")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Serializable] = constant(Serializable()).withId(id).addHints(hints)
}
