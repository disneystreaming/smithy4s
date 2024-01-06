package weather

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

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
