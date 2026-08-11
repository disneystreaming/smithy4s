package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class BlobInputNoMediaTypeOutput(data: Option[String] = None)

object BlobInputNoMediaTypeOutput extends ShapeTag.Companion[BlobInputNoMediaTypeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "BlobInputNoMediaTypeOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(data: Option[String]): BlobInputNoMediaTypeOutput = BlobInputNoMediaTypeOutput(data)

  implicit val schema: Schema[BlobInputNoMediaTypeOutput] = struct(
    string.optional[BlobInputNoMediaTypeOutput]("data", _.data).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
