package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

final case class CustomCodeOutput(code: Option[Int] = None)

object CustomCodeOutput extends ShapeTag.Companion[CustomCodeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CustomCodeOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[CustomCodeOutput] = struct(
    int.optional[CustomCodeOutput]("code", _.code).addHints(smithy.api.HttpResponseCode()),
  ){
    CustomCodeOutput.apply
  }.withId(id).addHints(hints)
}
