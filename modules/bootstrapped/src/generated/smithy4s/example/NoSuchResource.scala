package smithy4s.example

import smithy.api.Error
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NoSuchResource(resourceType: String) extends Throwable {
}
object NoSuchResource extends ShapeTag.$Companion[NoSuchResource] {
  val $id: ShapeId = ShapeId("smithy4s.example", "NoSuchResource")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
  )

  val resourceType: FieldLens[NoSuchResource, String] = string.required[NoSuchResource]("resourceType", _.resourceType, n => c => c.copy(resourceType = n)).addHints(Required())

  implicit val $schema: Schema[NoSuchResource] = struct(
    resourceType,
  ){
    NoSuchResource.apply
  }.withId($id).addHints($hints)
}
