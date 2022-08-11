package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class MixinErrorExample(a: Option[String]=None, b: Option[Int]=None, c: Option[Long]=None, d: Option[Boolean]=None) extends Throwable with CommonFieldsOne with CommonFieldsTwo {
  
}
object MixinErrorExample extends ShapeTag.Companion[MixinErrorExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinErrorExample")
  
  val hints : Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )
  
  implicit val schema: Schema[MixinErrorExample] = struct(
    string.optional[MixinErrorExample]("a", _.a),
    int.optional[MixinErrorExample]("b", _.b),
    long.optional[MixinErrorExample]("c", _.c),
    boolean.optional[MixinErrorExample]("d", _.d),
  ){
    MixinErrorExample.apply
  }.withId(id).addHints(hints)
}