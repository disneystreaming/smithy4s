package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloInput(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None)

object SayHelloInput extends ShapeTag.Companion[SayHelloInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  implicit val schema: Schema[SayHelloInput] = struct(
    string.optional[SayHelloInput]("greeting", _.greeting).addHints(smithy.api.HttpHeader("X-Greeting")),
    string.optional[SayHelloInput]("query", _.query).addHints(smithy.api.HttpQuery("Hi")),
    string.optional[SayHelloInput]("name", _.name),
  ){
    SayHelloInput.apply
  }.withId(id).addHints(hints)
}
