package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class DefaultAcceptHeaderOutput(result: Option[String] = None)

object DefaultAcceptHeaderOutput extends ShapeTag.Companion[DefaultAcceptHeaderOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "DefaultAcceptHeaderOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(result: Option[String]): DefaultAcceptHeaderOutput = DefaultAcceptHeaderOutput(result)

  implicit val schema: Schema[DefaultAcceptHeaderOutput] = struct(
    string.optional[DefaultAcceptHeaderOutput]("result", _.result).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
