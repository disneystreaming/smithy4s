package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class MixinExample(a: Option[String]=None, b: Option[Int]=None, c: Option[Long]=None, d: Option[Boolean]=None) extends CommonFieldsOne with CommonFieldsTwo
object MixinExample extends ShapeTag.Companion[MixinExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinExample")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[MixinExample] = struct(
    string.optional[MixinExample]("a", _.a),
    int.optional[MixinExample]("b", _.b),
    long.optional[MixinExample]("c", _.c),
    boolean.optional[MixinExample]("d", _.d),
  ){
    MixinExample.apply
  }.withId(id).addHints(hints)
}