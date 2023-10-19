package weather

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Dog(name: String)

object Dog extends ShapeTag.Companion[Dog] {
  val id: ShapeId = ShapeId("weather", "Dog")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Dog] = struct(
    string.required[Dog]("name", _.name),
  ){
    Dog.apply
  }.withId(id).addHints(hints)
}
