package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class OpticsStructure(two: Option[OpticsEnum] = None)
object OpticsStructure extends ShapeTag.Companion[OpticsStructure] {

  val two = OpticsEnum.schema.optional[OpticsStructure]("two", _.two, n => c => c.copy(two = n))

  implicit val schema: Schema[OpticsStructure] = struct(
    two,
  ){
    OpticsStructure.apply
  }
  .withId(ShapeId("smithy4s.example", "OpticsStructure"))
  .addHints(
    Hints.empty
  )
}
