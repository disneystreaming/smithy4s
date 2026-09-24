package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param image
  *   PNG image blob type
  */
final case class BlobInputWithMediaTypeInput(image: PngImage)

object BlobInputWithMediaTypeInput extends ShapeTag.Companion[BlobInputWithMediaTypeInput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "BlobInputWithMediaTypeInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(image: PngImage): BlobInputWithMediaTypeInput = BlobInputWithMediaTypeInput(image)

  implicit val schema: Schema[BlobInputWithMediaTypeInput] = struct(
    PngImage.schema.required[BlobInputWithMediaTypeInput]("image", _.image).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
