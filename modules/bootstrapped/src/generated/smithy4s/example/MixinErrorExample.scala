package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string

final case class MixinErrorExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends Smithy4sThrowable with CommonFieldsOne with CommonFieldsTwo {
}

object MixinErrorExample extends ShapeTag.Companion[MixinErrorExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinErrorExample")

  val hints: Hints = Hints(
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
