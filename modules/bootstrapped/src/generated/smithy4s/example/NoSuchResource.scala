package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NoSuchResource(resourceType: String) extends Throwable {
}
object NoSuchResource extends ShapeTag.Companion[NoSuchResource] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  val resourceType = string.required[NoSuchResource]("resourceType", _.resourceType).addHints(smithy.api.Required())

  implicit val schema: Schema[NoSuchResource] = struct(
    resourceType,
  ){
    NoSuchResource.apply
  }.withId(ShapeId("smithy4s.example", "NoSuchResource")).addHints(hints)
}
