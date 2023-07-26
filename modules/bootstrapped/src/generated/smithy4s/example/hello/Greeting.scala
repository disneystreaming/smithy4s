package smithy4s.example.hello

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Greeting(message: String)
object Greeting extends ShapeTag.Companion[Greeting] {

  val message: FieldLens[Greeting, String] = string.required[Greeting]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val schema: Schema[Greeting] = struct(
    message,
  ){
    Greeting.apply
  }
  .withId(ShapeId("smithy4s.example.hello", "Greeting"))
}
