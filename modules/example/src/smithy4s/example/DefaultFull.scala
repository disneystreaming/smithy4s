package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.int
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class DefaultFull(three: String, one: Int = 1, two: Option[String] = None)
object DefaultFull extends ShapeTag.Companion[DefaultFull] {
  val id: ShapeId = ShapeId("smithy4s.example", "DefaultFull")

  val hints : Hints = Hints.empty

  implicit val schema: Schema[DefaultFull] = struct(
    string.required[DefaultFull]("three", _.three).addHints(smithy.api.Required()),
    int.required[DefaultFull]("one", _.one).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.0))),
    string.optional[DefaultFull]("two", _.two),
  ){
    DefaultFull.apply
  }.withId(id).addHints(hints)
}