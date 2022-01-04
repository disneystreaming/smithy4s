package smithy4s.example

import java.util.UUID
import smithy4s.Newtype
import smithy4s.syntax._

object ObjectKey extends Newtype[UUID] {
  object T {
    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
      smithy4s.api.UuidFormat(),
    )
    val schema : smithy4s.Schema[UUID] = uuid.withHints(hints)
    implicit val staticSchema : schematic.Static[smithy4s.Schema[UUID]] = schematic.Static(schema)
  }
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ObjectKey")
  val hints: smithy4s.Hints = T.hints
  val schema : smithy4s.Schema[ObjectKey] = bijection(T.schema, ObjectKey(_), (_ : ObjectKey).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[ObjectKey]] = schematic.Static(schema)
}