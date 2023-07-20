package smithy4s.example.guides.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class World(message: String = "World !")
object World extends ShapeTag.Companion[World] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "World")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[World] = struct(
    string.required[World]("message", _.message).addHints(smithy.api.Default(smithy4s.Document.fromString("World !"))),
  ){
    World.apply
  }.withId(id).addHints(hints)
}
