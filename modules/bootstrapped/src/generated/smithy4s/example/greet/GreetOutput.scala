package smithy4s.example.greet

import smithy.api.Output
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetOutput(message: String)
object GreetOutput extends ShapeTag.Companion[GreetOutput] {

  val message = string.required[GreetOutput]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val schema: Schema[GreetOutput] = struct(
    message,
  ){
    GreetOutput.apply
  }
  .withId(ShapeId("smithy4s.example.greet", "GreetOutput"))
  .addHints(
    Hints(
      Output(),
    )
  )
}
