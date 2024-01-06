package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string

final case class MixinExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends CommonFieldsOne with CommonFieldsTwo

object MixinExample extends ShapeTag.Companion[MixinExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinExample")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[MixinExample] = struct(
    string.optional[MixinExample]("a", _.a),
    int.optional[MixinExample]("b", _.b),
    long.optional[MixinExample]("c", _.c),
    boolean.optional[MixinExample]("d", _.d),
  ){
    MixinExample.apply
  }.withId(id).addHints(hints)
}
