package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class Cons(head: Int, tail: ConsList)

object Cons extends ShapeTag.Companion[Cons] {
  val id: ShapeId = ShapeId("smithy4s.example", "Cons")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(head: Int, tail: ConsList): Cons = Cons(head, tail)

  implicit val schema: Schema[Cons] = recursive(struct(
    int.required[Cons]("head", _.head),
    ConsList.schema.required[Cons]("tail", _.tail),
  )(make).withId(id).addHints(hints))
}
