package smithy4s.example.content

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NoBodyOperationInput(query: Option[String] = None)

object NoBodyOperationInput extends ShapeTag.Companion[NoBodyOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example.content", "NoBodyOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(query: Option[String]): NoBodyOperationInput = NoBodyOperationInput(query)

  implicit val schema: Schema[NoBodyOperationInput] = struct(
    string.optional[NoBodyOperationInput]("query", _.query).addHints(smithy.api.HttpQuery("q")),
  )(make).withId(id).addHints(hints)
}
