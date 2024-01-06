package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class SayHelloInput(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None)

object SayHelloInput extends ShapeTag.Companion[SayHelloInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[SayHelloInput] = struct(
    string.optional[SayHelloInput]("greeting", _.greeting).addHints(smithy.api.HttpHeader("X-Greeting")),
    string.optional[SayHelloInput]("query", _.query).addHints(smithy.api.HttpQuery("Hi")),
    string.optional[SayHelloInput]("name", _.name),
  ){
    SayHelloInput.apply
  }.withId(id).addHints(hints)
}
