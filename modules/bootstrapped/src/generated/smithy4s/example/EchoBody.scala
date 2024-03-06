package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EchoBody(data: Option[String] = None)

object EchoBody extends ShapeTag.Companion[EchoBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "EchoBody")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(data: Option[String]): EchoBody = EchoBody(data)

  implicit val schema: Schema[EchoBody] = struct(
    string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[EchoBody]("data", _.data),
  )(make).withId(id).addHints(hints)
}
