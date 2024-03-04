package smithy4s.example.guides.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class World(message: String = "World !")

object World extends ShapeTag.Companion[World] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.hello", "World")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(message: String): World = World(message)

  implicit val schema: Schema[World] = struct(
    string.field[World]("message", _.message).addHints(smithy.api.Default(smithy4s.Document.fromString("World !"))),
  )(make).withId(id).addHints(hints)
}
