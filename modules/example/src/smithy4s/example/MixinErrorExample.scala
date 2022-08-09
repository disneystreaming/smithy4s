package smithy4s.example

import smithy4s.schema.Schema._

case class MixinErrorExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends Throwable with CommonFieldsOne with CommonFieldsTwo {
}
object MixinErrorExample extends smithy4s.ShapeTag.Companion[MixinErrorExample] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "MixinErrorExample")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: smithy4s.Schema[MixinErrorExample] = struct(
    string.optional[MixinErrorExample]("a", _.a),
    int.optional[MixinErrorExample]("b", _.b),
    long.optional[MixinErrorExample]("c", _.c),
    boolean.optional[MixinErrorExample]("d", _.d),
  ){
    MixinErrorExample.apply
  }.withId(id).addHints(hints)
}