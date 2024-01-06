package smithy4s.example.guides.auth

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class World(message: String = "World !")

object World extends ShapeTag.Companion[World] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "World")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[World] = struct(
    string.field[World]("message", _.message).addHints(smithy.api.Default(_root_.smithy4s.Document.fromString("World !"))),
  ){
    World.apply
  }.withId(id).addHints(hints)
}
