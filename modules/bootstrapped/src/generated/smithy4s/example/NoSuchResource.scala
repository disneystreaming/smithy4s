package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class NoSuchResource(resourceType: String) extends Smithy4sThrowable {
}

object NoSuchResource extends ShapeTag.Companion[NoSuchResource] {
  val id: ShapeId = ShapeId("smithy4s.example", "NoSuchResource")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[NoSuchResource] = struct(
    string.required[NoSuchResource]("resourceType", _.resourceType),
  ){
    NoSuchResource.apply
  }.withId(id).addHints(hints)
}
