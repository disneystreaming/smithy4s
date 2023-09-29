package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class OpticsStructure(two: Option[OpticsEnum] = None)

object OpticsStructure extends ShapeTag.Companion[OpticsStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "OpticsStructure")

  val hints: Hints = Hints.empty

  object optics {
    val two: Lens[OpticsStructure, Option[OpticsEnum]] = Lens[OpticsStructure, Option[OpticsEnum]](_.two)(n => a => a.copy(two = n))
  }

  implicit val schema: Schema[OpticsStructure] = struct(
    OpticsEnum.schema.optional[OpticsStructure]("two", _.two),
  ){
    OpticsStructure.apply
  }.withId(id).addHints(hints)
}
