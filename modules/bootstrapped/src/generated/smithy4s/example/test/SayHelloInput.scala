package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SayHelloInput(greeting: Option[String] = None, query: Option[String] = None, name: Option[String] = None)
object SayHelloInput extends ShapeTag.Companion[SayHelloInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "SayHelloInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  object Optics {
    val greeting = Lens[SayHelloInput, Option[String]](_.greeting)(n => a => a.copy(greeting = n))
    val query = Lens[SayHelloInput, Option[String]](_.query)(n => a => a.copy(query = n))
    val name = Lens[SayHelloInput, Option[String]](_.name)(n => a => a.copy(name = n))
  }

  implicit val schema: Schema[SayHelloInput] = struct(
    string.optional[SayHelloInput]("greeting", _.greeting).addHints(smithy.api.HttpHeader("X-Greeting")),
    string.optional[SayHelloInput]("query", _.query).addHints(smithy.api.HttpQuery("Hi")),
    string.optional[SayHelloInput]("name", _.name),
  ){
    SayHelloInput.apply
  }.withId(id).addHints(hints)
}
