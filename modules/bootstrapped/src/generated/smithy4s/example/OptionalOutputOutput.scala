package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class OptionalOutputOutput(body: Option[String] = None)

object OptionalOutputOutput extends ShapeTag.Companion[OptionalOutputOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "OptionalOutputOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[OptionalOutputOutput] = struct(
    string.optional[OptionalOutputOutput]("body", _.body).addHints(smithy.api.HttpPayload()),
  ){
    OptionalOutputOutput.apply
  }.withId(id).addHints(hints)
}
