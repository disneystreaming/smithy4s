package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class EchoBody(data: Option[String] = None)

object EchoBody extends ShapeTag.Companion[EchoBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "EchoBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[EchoBody] = struct(
    string.validated(smithy.api.Length(min = Some(10L), max = None)).optional[EchoBody]("data", _.data),
  ){
    EchoBody.apply
  }.withId(id).addHints(hints)
}
