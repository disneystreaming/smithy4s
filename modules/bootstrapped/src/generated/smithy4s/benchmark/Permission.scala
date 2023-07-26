package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.struct

final case class Permission(read: Option[Boolean] = None, write: Option[Boolean] = None, directory: Option[Boolean] = None)
object Permission extends ShapeTag.$Companion[Permission] {
  val $id: ShapeId = ShapeId("smithy4s.benchmark", "Permission")

  val $hints: Hints = Hints.empty

  val read: FieldLens[Permission, Option[Boolean]] = boolean.optional[Permission]("read", _.read, n => c => c.copy(read = n))
  val write: FieldLens[Permission, Option[Boolean]] = boolean.optional[Permission]("write", _.write, n => c => c.copy(write = n))
  val directory: FieldLens[Permission, Option[Boolean]] = boolean.optional[Permission]("directory", _.directory, n => c => c.copy(directory = n))

  implicit val $schema: Schema[Permission] = struct(
    read,
    write,
    directory,
  ){
    Permission.apply
  }.withId($id).addHints($hints)
}