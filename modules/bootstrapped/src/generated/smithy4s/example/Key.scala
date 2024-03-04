package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Key(key: String)

object Key extends ShapeTag.Companion[Key] {
  val id: ShapeId = ShapeId("smithy4s.example", "Key")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(key: String): Key = Key(key)

  implicit val schema: Schema[Key] = struct(
    string.required[Key]("key", _.key),
  )(make).withId(id).addHints(hints)
}
