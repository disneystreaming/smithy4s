package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class GetStatusCodeOutput(code: Int)

object GetStatusCodeOutput extends ShapeTag.Companion[GetStatusCodeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetStatusCodeOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(code: Int): GetStatusCodeOutput = GetStatusCodeOutput(code)

  implicit val schema: Schema[GetStatusCodeOutput] = struct[GetStatusCodeOutput](
    int.required[GetStatusCodeOutput]("code", _.code).addHints(Hints.dynamic(ShapeId("smithy.api", "httpResponseCode"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
