package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NoSuchResource(resourceType: String) extends Smithy4sThrowable {
}

object NoSuchResource extends ShapeTag.Companion[NoSuchResource] {
  val id: ShapeId = ShapeId("smithy4s.example", "NoSuchResource")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(resourceType: String): NoSuchResource = NoSuchResource(resourceType)

  implicit val schema: Schema[NoSuchResource] = struct(
    string.required[NoSuchResource]("resourceType", _.resourceType),
  )(make).withId(id).addHints(hints)
}
