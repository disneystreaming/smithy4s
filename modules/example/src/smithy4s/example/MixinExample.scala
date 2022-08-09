package smithy4s.example

import smithy4s.schema.Schema._

case class MixinExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends CommonFieldsOne with CommonFieldsTwo
object MixinExample extends smithy4s.ShapeTag.Companion[MixinExample] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "MixinExample")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[MixinExample] = struct(
    string.optional[MixinExample]("a", _.a),
    int.optional[MixinExample]("b", _.b),
    long.optional[MixinExample]("c", _.c),
    boolean.optional[MixinExample]("d", _.d),
  ){
    MixinExample.apply
  }.withId(id).addHints(hints)
}