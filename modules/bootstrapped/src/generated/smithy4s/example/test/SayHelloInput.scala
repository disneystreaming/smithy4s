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
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(greeting: Option[String], query: Option[String], name: Option[String]): SayHelloInput = SayHelloInput(greeting, query, name)

  implicit val schema: Schema[SayHelloInput] = struct(
    string.optional[SayHelloInput]("greeting", _.greeting).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("X-Greeting"))),
    string.optional[SayHelloInput]("query", _.query).addHints(Hints.dynamic(ShapeId("smithy.api", "httpQuery"), smithy4s.Document.fromString("Hi"))),
    string.optional[SayHelloInput]("name", _.name),
  )(make).withId(id).addHints(hints)
}
