package smithy4s.example

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.refined.Age.provider._
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class StructureWithRefinedMember(otherAge: Option[smithy4s.refined.Age] = None)
object StructureWithRefinedMember extends ShapeTag.Companion[StructureWithRefinedMember] {

  val otherAge: FieldLens[StructureWithRefinedMember, Option[smithy4s.refined.Age]] = int.refined[smithy4s.refined.Age](AgeFormat()).optional[StructureWithRefinedMember]("otherAge", _.otherAge, n => c => c.copy(otherAge = n)).addHints(AgeFormat())

  implicit val schema: Schema[StructureWithRefinedMember] = struct(
    otherAge,
  ){
    StructureWithRefinedMember.apply
  }
  .withId(ShapeId("smithy4s.example", "StructureWithRefinedMember"))
}
