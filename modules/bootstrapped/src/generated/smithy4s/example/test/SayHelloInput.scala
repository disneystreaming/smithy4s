package smithy4s.example.test

import smithy.api.HttpHeader
import smithy.api.HttpQuery
import smithy.api.Input
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloInput(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None)
object SayHelloInput extends ShapeTag.$Companion[SayHelloInput] {
  val $id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloInput")

  val $hints: Hints = Hints(
    Input(),
  )

  val greeting: FieldLens[SayHelloInput, Option[String]] = string.optional[SayHelloInput]("greeting", _.greeting, n => c => c.copy(greeting = n)).addHints(HttpHeader("X-Greeting"))
  val query: FieldLens[SayHelloInput, Option[String]] = string.optional[SayHelloInput]("query", _.query, n => c => c.copy(query = n)).addHints(HttpQuery("Hi"))
  val name: FieldLens[SayHelloInput, Option[String]] = string.optional[SayHelloInput]("name", _.name, n => c => c.copy(name = n))

  implicit val $schema: Schema[SayHelloInput] = struct(
    greeting,
    query,
    name,
  ){
    SayHelloInput.apply
  }.withId($id).addHints($hints)
}