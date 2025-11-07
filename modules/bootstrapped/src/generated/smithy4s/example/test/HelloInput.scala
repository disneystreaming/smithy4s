package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HelloInput(name: String)

object HelloInput extends ShapeTag.Companion[HelloInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(name: String): HelloInput = HelloInput(name)

  implicit val schema: Schema[HelloInput] = struct(
    string.required[HelloInput]("name", _.name).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
