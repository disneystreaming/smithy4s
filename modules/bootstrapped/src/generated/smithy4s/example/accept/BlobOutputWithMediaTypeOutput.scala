package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param image
  *   PNG image blob type
  */
final case class BlobOutputWithMediaTypeOutput(image: Option[PngImage] = None)

object BlobOutputWithMediaTypeOutput extends ShapeTag.Companion[BlobOutputWithMediaTypeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "BlobOutputWithMediaTypeOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(image: Option[PngImage]): BlobOutputWithMediaTypeOutput = BlobOutputWithMediaTypeOutput(image)

  implicit val schema: Schema[BlobOutputWithMediaTypeOutput] = struct(
    PngImage.schema.optional[BlobOutputWithMediaTypeOutput]("image", _.image).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
