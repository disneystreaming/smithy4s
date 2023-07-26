package smithy4s.example

import smithy.api.Error
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

  val a = string.optional[MixinErrorExample]("a", _.a, n => c => c.copy(a = n))
  val b = int.optional[MixinErrorExample]("b", _.b, n => c => c.copy(b = n))
  val c = long.optional[MixinErrorExample]("c", _.c, n => c => c.copy(c = n))
  val d = boolean.optional[MixinErrorExample]("d", _.d, n => c => c.copy(d = n))

  implicit val schema: Schema[MixinErrorExample] = struct(
    a,
    b,
    c,
    d,
  ){
    MixinErrorExample.apply
  }
  .withId(ShapeId("smithy4s.example", "MixinErrorExample"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
    )
  )
}
