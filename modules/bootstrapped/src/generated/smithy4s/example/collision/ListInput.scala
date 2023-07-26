package smithy4s.example.collision

import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ListInput(list: List[String])
object ListInput extends ShapeTag.Companion[ListInput] {

  val list = MyList.underlyingSchema.required[ListInput]("list", _.list, n => c => c.copy(list = n)).addHints(Required())

  implicit val schema: Schema[ListInput] = struct(
    list,
  ){
    ListInput.apply
  }
  .withId(ShapeId("smithy4s.example.collision", "ListInput"))
  .addHints(
    Hints(
      Input(),
    )
  )
}
