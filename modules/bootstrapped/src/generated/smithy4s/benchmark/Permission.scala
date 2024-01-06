package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.boolean

final case class Permission(read: Option[Boolean] = None, write: Option[Boolean] = None, directory: Option[Boolean] = None)

object Permission extends ShapeTag.Companion[Permission] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "Permission")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Permission] = struct(
    boolean.optional[Permission]("read", _.read),
    boolean.optional[Permission]("write", _.write),
    boolean.optional[Permission]("directory", _.directory),
  ){
    Permission.apply
  }.withId(id).addHints(hints)
}
