package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Packagee(_class: Option[Int] = None)

object Packagee extends ShapeTag.Companion[Packagee] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "Packagee")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(_class: Option[Int]): Packagee = Packagee(_class)

  implicit val schema: Schema[Packagee] = struct(
    int.optional[Packagee]("class", _._class),
  )(make).withId(id).addHints(hints)
}
