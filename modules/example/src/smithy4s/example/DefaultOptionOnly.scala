package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.int
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class DefaultOptionOnly(one: Int, three: String, two: Option[String] = None)
object DefaultOptionOnly extends ShapeTag.Companion[DefaultOptionOnly] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultOptionOnly")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[DefaultOptionOnly] = struct(
    int.required[DefaultOptionOnly]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0))),
    string.required[DefaultOptionOnly]("three", _.three).addHints(smithy.api.Required()),
    string.optional[DefaultOptionOnly]("two", _.two),
  ){
    DefaultOptionOnly.apply
  }.withId(id).addHints(hints)
}