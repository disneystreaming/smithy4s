package smithy4s.example

import smithy.api.HttpPayload
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetMenuResult(menu: Map[String, MenuItem])
object GetMenuResult extends ShapeTag.Companion[GetMenuResult] {

  val menu: FieldLens[GetMenuResult, Map[String, MenuItem]] = Menu.underlyingSchema.required[GetMenuResult]("menu", _.menu, n => c => c.copy(menu = n)).addHints(HttpPayload(), Required())

  implicit val schema: Schema[GetMenuResult] = struct(
    menu,
  ){
    GetMenuResult.apply
  }
  .withId(ShapeId("smithy4s.example", "GetMenuResult"))
}
