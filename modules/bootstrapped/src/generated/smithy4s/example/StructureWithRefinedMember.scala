package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedMember(otherAge: Option[smithy4s.refined.Age] = None)
object StructureWithRefinedMember extends ShapeTag.Companion[StructureWithRefinedMember] {
  val id: ShapeId = ShapeId("smithy4s.example", "StructureWithRefinedMember")

  val hints: Hints = Hints.empty

  object Optics {
    val otherAge = Lens[StructureWithRefinedMember, Option[smithy4s.refined.Age]](_.otherAge)(n => a => a.copy(otherAge = n))
  }

  implicit val schema: Schema[StructureWithRefinedMember] = struct(
    int.refined[smithy4s.refined.Age](smithy4s.example.AgeFormat()).optional[StructureWithRefinedMember]("otherAge", _.otherAge).addHints(smithy4s.example.AgeFormat()),
  ){
    StructureWithRefinedMember.apply
  }.withId(id).addHints(hints)
}
