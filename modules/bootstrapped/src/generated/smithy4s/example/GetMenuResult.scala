package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class GetMenuResult(menu: Map[String, MenuItem])
object GetMenuResult extends ShapeTag.Companion[GetMenuResult] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetMenuResult")

  val hints: Hints = Hints.empty

  object Lenses {
    val menu = Lens[GetMenuResult, Map[String, MenuItem]](_.menu)(n => a => a.copy(menu = n))
  }

  implicit val schema: Schema[GetMenuResult] = struct(
    Menu.underlyingSchema.required[GetMenuResult]("menu", _.menu).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    GetMenuResult.apply
  }.withId(id).addHints(hints)
}
