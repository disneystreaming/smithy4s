package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class RecursiveMember()

object RecursiveMember extends ShapeTag.Companion[RecursiveMember] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "RecursiveMember")

  val hints: Hints = Hints(
    smithy4s.example.traits.IndirectRecursiveViaMembersTrait(member = None),
  )

  implicit val schema: Schema[RecursiveMember] = constant(RecursiveMember()).withId(id).addHints(hints)
}
