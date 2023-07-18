package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetStreamedObjectInput(key: String)
object GetStreamedObjectInput extends ShapeTag.Companion[GetStreamedObjectInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStreamedObjectInput")

  val hints: Hints = Hints.empty

  object Lenses {
    val key = Lens[GetStreamedObjectInput, String](_.key)(n => a => a.copy(key = n))
  }

  implicit val schema: Schema[GetStreamedObjectInput] = struct(
    string.required[GetStreamedObjectInput]("key", _.key).addHints(smithy.api.Required()),
  ){
    GetStreamedObjectInput.apply
  }.withId(id).addHints(hints)
}
