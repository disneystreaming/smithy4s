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

  // constructor using the original order from the spec
  private def make(astring: AString): AStructure = AStructure(astring)

  implicit val schema: Schema[AStructure] = struct(
    AString.schema.field[AStructure]("astring", _.astring).addHints(smithy.api.Default(smithy4s.Document.fromString("\"Hello World\" with \"quotes\""))),
  )(make).withId(id).addHints(hints)
}
