package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class ListInput(list: List[String])
object ListInput extends ShapeTag.Companion[ListInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ListInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ListInput] = struct(
    MyList.underlyingSchema.required[ListInput]("list", _.list).addHints(smithy.api.Required()),
  ){
    ListInput.apply
  }.withId(id).addHints(hints)
}