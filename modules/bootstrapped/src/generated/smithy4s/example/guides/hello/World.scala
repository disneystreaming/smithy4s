package smithy4s.example.guides.hello

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class World(message: String = "World !")
object World extends ShapeTag.$Companion[World] {
  val $id: ShapeId = ShapeId("smithy4s.example.guides.hello", "World")

  val $hints: Hints = Hints.empty

  val message: FieldLens[World, String] = string.required[World]("message", _.message, n => c => c.copy(message = n)).addHints(Default(smithy4s.Document.fromString("World !")))

  implicit val $schema: Schema[World] = struct(
    message,
  ){
    World.apply
  }.withId($id).addHints($hints)
}
