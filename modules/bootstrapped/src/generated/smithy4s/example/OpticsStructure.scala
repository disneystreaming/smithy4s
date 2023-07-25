package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class OpticsStructure(two: Option[OpticsEnum] = None)
object OpticsStructure extends ShapeTag.Companion[OpticsStructure] {
  val hints: Hints = Hints.empty

  object Optics {
    val two: Lens[OpticsStructure, Option[OpticsEnum]] = Lens[OpticsStructure, Option[OpticsEnum]](_.two)(n => a => a.copy(two = n))
  }

  val two = OpticsEnum.schema.optional[OpticsStructure]("two", _.two)

  implicit val schema: Schema[OpticsStructure] = struct(
    two,
  ){
    OpticsStructure.apply
  }.withId(ShapeId("smithy4s.example", "OpticsStructure")).addHints(hints)
}
