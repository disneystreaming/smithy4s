package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class IntList(head: Int, tail: Option[smithy4s.example.IntList] = None)
object IntList extends ShapeTag.Companion[IntList] {
  val id: ShapeId = ShapeId("smithy4s.example", "IntList")

  val hints: Hints = Hints.empty

  object Optics {
    val head = Lens[IntList, Int](_.head)(n => a => a.copy(head = n))
    val tail = Lens[IntList, Option[smithy4s.example.IntList]](_.tail)(n => a => a.copy(tail = n))
  }

  implicit val schema: Schema[IntList] = recursive(struct(
    int.required[IntList]("head", _.head).addHints(smithy.api.Required()),
    smithy4s.example.IntList.schema.optional[IntList]("tail", _.tail),
  ){
    IntList.apply
  }.withId(id).addHints(hints))
}
