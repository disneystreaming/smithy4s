package smithy4s.example

import scala.runtime.ScalaRunTime
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NoSuchResource(resourceType: String) extends Throwable {
  override def toString(): String = ScalaRunTime._toString(this)
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
