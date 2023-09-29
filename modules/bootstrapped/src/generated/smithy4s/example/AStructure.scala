package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

/** @param astring
  *   This is a simple example of a "quoted string"
  */
final case class AStructure(astring: AString = smithy4s.example.AString("\"Hello World\" with \"quotes\""))

object AStructure extends ShapeTag.Companion[AStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "AStructure")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[AStructure] = struct(
    AString.schema.required[AStructure]("astring", _.astring).addHints(smithy.api.Default(smithy4s.Document.fromString("\"Hello World\" with \"quotes\""))),
  ){
    AStructure.apply
  }.withId(id).addHints(hints)
}
