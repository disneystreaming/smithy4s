package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class ListInput(list: List[String])

object ListInput extends ShapeTag.Companion[ListInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ListInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ListInput] = struct(
    MyList.underlyingSchema.required[ListInput]("list", _.list),
  ){
    ListInput.apply
  }.withId(id).addHints(hints)
}
