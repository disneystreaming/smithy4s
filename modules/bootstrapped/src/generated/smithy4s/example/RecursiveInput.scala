package smithy4s.example

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class RecursiveInput(hello: Option[smithy4s.example.RecursiveInput] = None)
object RecursiveInput extends ShapeTag.Companion[RecursiveInput] {

  implicit val schema: Schema[RecursiveInput] = recursive(struct(
    hello,
  ){
    RecursiveInput.apply
  }
  .withId(ShapeId("smithy4s.example", "RecursiveInput")))

  val hello: FieldLens[RecursiveInput, Option[smithy4s.example.RecursiveInput]] = smithy4s.example.RecursiveInput.schema.optional[RecursiveInput]("hello", _.hello, n => c => c.copy(hello = n))
}
