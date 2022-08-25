package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.int
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class DefaultNone(one: Int, two: Option[String], three: String)
object DefaultNone extends ShapeTag.Companion[DefaultNone] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultNone")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[DefaultNone] = struct(
    int.required[DefaultNone]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0))),
    string.optional[DefaultNone]("two", _.two).addHints(),
    string.required[DefaultNone]("three", _.three).addHints(smithy.api.Required()),
  ){
    DefaultNone.apply
  }.withId(id).addHints(hints)
}