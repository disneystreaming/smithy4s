package smithy4s.example.reservedNameOverride

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Set(someField: String, otherField: Int)

object Set extends ShapeTag.Companion[Set] {
  val id: ShapeId = ShapeId("smithy4s.example.reservedNameOverride", "Set")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(someField: String, otherField: Int): Set = Set(someField, otherField)

  implicit val schema: Schema[Set] = struct(
    string.required[Set]("someField", _.someField),
    int.required[Set]("otherField", _.otherField),
  )(make).withId(id).addHints(hints)
}
