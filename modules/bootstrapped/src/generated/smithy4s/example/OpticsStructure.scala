package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.optics.Lens

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
