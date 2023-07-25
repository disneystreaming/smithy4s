package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloInput(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None)
object SayHelloInput extends ShapeTag.Companion[SayHelloInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val greeting = string.optional[SayHelloInput]("greeting", _.greeting).addHints(smithy.api.HttpHeader("X-Greeting"))
  val query = string.optional[SayHelloInput]("query", _.query).addHints(smithy.api.HttpQuery("Hi"))
  val name = string.optional[SayHelloInput]("name", _.name)

  implicit val schema: Schema[SayHelloInput] = struct(
    greeting,
    query,
    name,
  ){
    SayHelloInput.apply
  }.withId(ShapeId("smithy4s.example.test", "SayHelloInput")).addHints(hints)
}
