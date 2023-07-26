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

final case class MixinExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends CommonFieldsOne with CommonFieldsTwo
object MixinExample extends ShapeTag.Companion[MixinExample] {

  val a = string.optional[MixinExample]("a", _.a, n => c => c.copy(a = n))
  val b = int.optional[MixinExample]("b", _.b, n => c => c.copy(b = n))
  val c = long.optional[MixinExample]("c", _.c, n => c => c.copy(c = n))
  val d = boolean.optional[MixinExample]("d", _.d, n => c => c.copy(d = n))

  implicit val schema: Schema[MixinExample] = struct(
    a,
    b,
    c,
    d,
  ){
    MixinExample.apply
  }
  .withId(ShapeId("smithy4s.example", "MixinExample"))
  .addHints(
    Hints.empty
  )
}
