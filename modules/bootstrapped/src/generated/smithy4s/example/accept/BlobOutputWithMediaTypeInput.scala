package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class BlobOutputWithMediaTypeInput(data: Option[String] = None)

object BlobOutputWithMediaTypeInput extends ShapeTag.Companion[BlobOutputWithMediaTypeInput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "BlobOutputWithMediaTypeInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: Option[String]): BlobOutputWithMediaTypeInput = BlobOutputWithMediaTypeInput(data)

  implicit val schema: Schema[BlobOutputWithMediaTypeInput] = struct(
    string.optional[BlobOutputWithMediaTypeInput]("data", _.data).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
