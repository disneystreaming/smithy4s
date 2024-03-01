package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class CustomCodeOutput(code: Option[Int] = None)

object CustomCodeOutput extends ShapeTag.Companion[CustomCodeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CustomCodeOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(code: Option[Int]): CustomCodeOutput = CustomCodeOutput(code)

  implicit val schema: Schema[CustomCodeOutput] = struct(
    int.optional[CustomCodeOutput]("code", _.code).addHints(smithy.api.HttpResponseCode()),
  ){
    make
  }.withId(id).addHints(hints)
}
