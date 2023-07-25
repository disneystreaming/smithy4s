package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MixinErrorExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends Throwable with CommonFieldsOne with CommonFieldsTwo {
}
object MixinErrorExample extends ShapeTag.Companion[MixinErrorExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinErrorExample")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  val a = string.optional[MixinErrorExample]("a", _.a)
  val b = int.optional[MixinErrorExample]("b", _.b)
  val c = long.optional[MixinErrorExample]("c", _.c)
  val d = boolean.optional[MixinErrorExample]("d", _.d)

  implicit val schema: Schema[MixinErrorExample] = struct(
    a,
    b,
    c,
    d,
  ){
    MixinErrorExample.apply
  }.withId(id).addHints(hints)
}
