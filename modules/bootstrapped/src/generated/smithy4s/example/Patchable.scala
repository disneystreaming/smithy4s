package smithy4s.example

import smithy4s.Hints
import smithy4s.Removable
import smithy4s.Removable.absent
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Patchable(a: Int, b: Option[Int] = None, c: Removable[Int] = absent)

object Patchable extends ShapeTag.Companion[Patchable] {
  val id: ShapeId = ShapeId("smithy4s.example", "Patchable")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Patchable] = struct(
    int.required[Patchable]("a", _.a),
    int.optional[Patchable]("b", _.b),
    int.removable[Patchable]("c", _.c).addHints(alloy.Nullable()),
  ){
    Patchable.apply
  }.withId(id).addHints(hints)
}
