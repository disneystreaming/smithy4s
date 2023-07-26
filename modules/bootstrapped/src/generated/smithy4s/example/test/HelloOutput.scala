package smithy4s.example.test

import smithy.api.Output
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HelloOutput(message: String)
object HelloOutput extends ShapeTag.Companion[HelloOutput] {

  val message = string.required[HelloOutput]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val schema: Schema[HelloOutput] = struct(
    message,
  ){
    HelloOutput.apply
  }
  .withId(ShapeId("smithy4s.example.test", "HelloOutput"))
  .addHints(
    Hints(
      Output(),
    )
  )
}
