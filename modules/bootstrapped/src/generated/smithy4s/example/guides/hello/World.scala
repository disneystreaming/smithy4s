package smithy4s.example.guides.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class World(message: String = "World !")
object World extends ShapeTag.Companion[World] {
  val hints: Hints = Hints.empty

  val message = string.required[World]("message", _.message).addHints(smithy.api.Default(smithy4s.Document.fromString("World !")))

  implicit val schema: Schema[World] = struct(
    message,
  ){
    World.apply
  }.withId(ShapeId("smithy4s.example.guides.hello", "World")).addHints(hints)
}
