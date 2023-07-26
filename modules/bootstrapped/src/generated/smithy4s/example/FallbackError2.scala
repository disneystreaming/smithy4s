package smithy4s.example

import smithy.api.Error
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class FallbackError2(error: String) extends Throwable {
}
object FallbackError2 extends ShapeTag.Companion[FallbackError2] {

  val error: FieldLens[FallbackError2, String] = string.required[FallbackError2]("error", _.error, n => c => c.copy(error = n)).addHints(Required())

  implicit val schema: Schema[FallbackError2] = struct(
    error,
  ){
    FallbackError2.apply
  }
  .withId(ShapeId("smithy4s.example", "FallbackError2"))
  .addHints(
    Error.CLIENT.widen,
  )
}
