package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ListInput(list: List[String])
object ListInput extends ShapeTag.Companion[ListInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val list = MyList.underlyingSchema.required[ListInput]("list", _.list).addHints(smithy.api.Required())

  implicit val schema: Schema[ListInput] = struct(
    list,
  ){
    ListInput.apply
  }.withId(ShapeId("smithy4s.example.collision", "ListInput")).addHints(hints)
}
