package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class BlobOutputNoMediaTypeInput(data: Option[String] = None)

object BlobOutputNoMediaTypeInput extends ShapeTag.Companion[BlobOutputNoMediaTypeInput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "BlobOutputNoMediaTypeInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: Option[String]): BlobOutputNoMediaTypeInput = BlobOutputNoMediaTypeInput(data)

  implicit val schema: Schema[BlobOutputNoMediaTypeInput] = struct(
    string.optional[BlobOutputNoMediaTypeInput]("data", _.data).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
