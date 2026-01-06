package smithy4s.example.accept

import smithy4s.Blob
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bytes
import smithy4s.schema.Schema.struct

final case class BlobOutputNoMediaTypeOutput(image: Option[Blob] = None)

object BlobOutputNoMediaTypeOutput extends ShapeTag.Companion[BlobOutputNoMediaTypeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "BlobOutputNoMediaTypeOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(image: Option[Blob]): BlobOutputNoMediaTypeOutput = BlobOutputNoMediaTypeOutput(image)

  implicit val schema: Schema[BlobOutputNoMediaTypeOutput] = struct(
    bytes.optional[BlobOutputNoMediaTypeOutput]("image", _.image).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
