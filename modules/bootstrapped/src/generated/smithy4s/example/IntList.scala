package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class IntList(head: Int, tail: Option[smithy4s.example.IntList] = None)
object IntList extends ShapeTag.$Companion[IntList] {
  val $id: ShapeId = ShapeId("smithy4s.example", "IntList")

  val $hints: Hints = Hints.empty

  implicit val $schema: Schema[IntList] = recursive(struct(
    head,
    tail,
  ){
    IntList.apply
  }.withId($id).addHints($hints))

  val head: FieldLens[IntList, Int] = int.required[IntList]("head", _.head, n => c => c.copy(head = n)).addHints(Required())
  val tail: FieldLens[IntList, Option[smithy4s.example.IntList]] = smithy4s.example.IntList.$schema.optional[IntList]("tail", _.tail, n => c => c.copy(tail = n))
}
