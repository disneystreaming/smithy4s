package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MixinExample(a: Option[String] = None, b: Option[Int] = None, c: Option[Long] = None, d: Option[Boolean] = None) extends CommonFieldsOne with CommonFieldsTwo
object MixinExample extends ShapeTag.Companion[MixinExample] {
  val id: ShapeId = ShapeId("smithy4s.example", "MixinExample")

  val hints: Hints = Hints.empty

  object Lenses {
    val a = Lens[MixinExample, Option[String]](_.a)(n => a => a.copy(a = n))
    val b = Lens[MixinExample, Option[Int]](_.b)(n => a => a.copy(b = n))
    val c = Lens[MixinExample, Option[Long]](_.c)(n => a => a.copy(c = n))
    val d = Lens[MixinExample, Option[Boolean]](_.d)(n => a => a.copy(d = n))
  }

  implicit val schema: Schema[MixinExample] = struct(
    string.optional[MixinExample]("a", _.a),
    int.optional[MixinExample]("b", _.b),
    long.optional[MixinExample]("c", _.c),
    boolean.optional[MixinExample]("d", _.d),
  ){
    MixinExample.apply
  }.withId(id).addHints(hints)
}
