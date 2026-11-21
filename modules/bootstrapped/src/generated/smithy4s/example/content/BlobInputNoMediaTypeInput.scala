package smithy4s.example.content

import smithy4s.Blob
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.struct

final case class BlobInputNoMediaTypeInput(image: Blob)

object BlobInputNoMediaTypeInput extends ShapeTag.Companion[BlobInputNoMediaTypeInput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "BlobInputNoMediaTypeInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(image: Blob): BlobInputNoMediaTypeInput = BlobInputNoMediaTypeInput(image)

  implicit val schema: Schema[BlobInputNoMediaTypeInput] = struct(
    bytes.required[BlobInputNoMediaTypeInput]("image", _.image).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
