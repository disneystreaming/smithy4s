package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedMember(otherAge: Option[smithy4s.refined.Age] = None)

object StructureWithRefinedMember extends ShapeTag.Companion[StructureWithRefinedMember] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedMember")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(otherAge: Option[smithy4s.refined.Age]): StructureWithRefinedMember = StructureWithRefinedMember(otherAge)

  implicit val schema: Schema[StructureWithRefinedMember] = struct(
    int.refined[smithy4s.refined.Age](smithy4s.example.AgeFormat()).optional[StructureWithRefinedMember]("otherAge", _.otherAge),
  ){
    make
  }.withId(id).addHints(hints)
}
