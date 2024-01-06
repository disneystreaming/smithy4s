package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.string

object PersonEmail extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "PersonEmail")
  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[PersonEmail] = bijection(underlyingSchema, asBijection)

  implicit val personEmailHash: cats.Hash[PersonEmail] = SchemaVisitorHash.fromSchema(schema)
}
