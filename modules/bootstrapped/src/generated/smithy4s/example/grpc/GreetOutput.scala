package smithy4s.example.grpc

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetOutput(greeting: String)

object GreetOutput extends ShapeTag.Companion[GreetOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.grpc", "GreetOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(greeting: String): GreetOutput = GreetOutput(greeting)

  implicit val schema: Schema[GreetOutput] = struct(
    string.required[GreetOutput]("greeting", _.greeting),
  )(make).withId(id).addHints(hints)
}
