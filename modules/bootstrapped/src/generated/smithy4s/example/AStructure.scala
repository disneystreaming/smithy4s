package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

/** @param astring
  *   This is a simple example of a "quoted string"
  */
final case class AStructure(astring: AString = AString("\"Hello World\" with \"quotes\""))
object AStructure extends ShapeTag.$Companion[AStructure] {
  val $id: ShapeId = ShapeId("smithy4s.example", "AStructure")

  val $hints: Hints = Hints.empty

  val astring: FieldLens[AStructure, AString] = AString.$schema.required[AStructure]("astring", _.astring, n => c => c.copy(astring = n)).addHints(Default(smithy4s.Document.fromString("\"Hello World\" with \"quotes\"")))

  implicit val $schema: Schema[AStructure] = struct(
    astring,
  ){
    AStructure.apply
  }.withId($id).addHints($hints)
}
