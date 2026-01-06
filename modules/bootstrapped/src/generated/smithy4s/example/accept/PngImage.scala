package smithy4s.example.accept

import smithy4s.Blob
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.bytes

/** PNG image blob type */
object PngImage extends Newtype[Blob] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "PngImage")
  val hints: Hints = Hints(
    smithy.api.Documentation("PNG image blob type"),
    smithy.api.MediaType("image/png"),
  ).lazily
  val underlyingSchema: Schema[Blob] = bytes.withId(id).addHints(hints)
  implicit val schema: Schema[PngImage] = bijection(underlyingSchema, asBijection)
}
