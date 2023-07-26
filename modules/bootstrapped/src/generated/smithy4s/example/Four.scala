package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Four(four: Int)
object Four extends ShapeTag.Companion[Four] {

  val four: FieldLens[Four, Int] = int.required[Four]("four", _.four, n => c => c.copy(four = n)).addHints(Required())

  implicit val schema: Schema[Four] = struct(
    four,
  ){
    Four.apply
  }
  .withId(ShapeId("smithy4s.example", "Four"))
}
